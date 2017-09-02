package kr.co.future.httpd.netty.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.util.Signal;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;

import java.util.List;

public abstract class KrakenReplayingDecoder<S> extends KrakenByteToMessageDecoder{
	public static final Signal REPLAY = Signal.valueOf(KrakenReplayingDecoder.class.getName() + ".REPLAY");

   private final KrakenReplayingDecoderBuffer replayable = new KrakenReplayingDecoderBuffer();
   private S state;
   private int checkpoint = -1;

   /**
    * Creates a new instance with no initial state (i.e: {@code null}).
    */
   protected KrakenReplayingDecoder() {
       this(null);
   }

   /**
    * Creates a new instance with the specified initial state.
    */
   protected KrakenReplayingDecoder(S initialState) {
       state = initialState;
   }

   /**
    * Stores the internal cumulative buffer's reader position.
    */
   protected void checkpoint() {
       checkpoint = internalBuffer().readerIndex();
   }

   /**
    * Stores the internal cumulative buffer's reader position and updates
    * the current decoder state.
    */
   protected void checkpoint(S state) {
       checkpoint();
       state(state);
   }

   /**
    * Returns the current state of this decoder.
    * @return the current state of this decoder
    */
   protected S state() {
       return state;
   }

   /**
    * Sets the current state of this decoder.
    * @return the old state of this decoder
    */
   protected S state(S newState) {
       S oldState = state;
       state = newState;
       return oldState;
   }
   
   @Override
   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
       RecyclableArrayList out = RecyclableArrayList.newInstance();
       try {
           replayable.terminate();
           callDecode(ctx, internalBuffer(), out);
           decodeLast(ctx, replayable, out);
       } catch (Signal replay) {
           // Ignore
           replay.expect(REPLAY);
       } catch (DecoderException e) {
           throw e;
       } catch (Exception e) {
           throw new DecoderException(e);
       } finally {
           try {
               if (cumulation != null) {
                   cumulation.release();
                   cumulation = null;
               }
               int size = out.size();
               for (int i = 0; i < size; i++) {
                   ctx.fireChannelRead(out.get(i));
               }
               if (size > 0) {
                   // Something was read, call fireChannelReadComplete()
                   ctx.fireChannelReadComplete();
               }
               ctx.fireChannelInactive();
           } finally {
               // recycle in all cases
               out.recycle();
           }
       }
   }

   @Override
   protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
       replayable.setCumulation(in);
       try {
           while (in.isReadable()) {
               int oldReaderIndex = checkpoint = in.readerIndex();
               int outSize = out.size();
               S oldState = state;
               int oldInputLength = in.readableBytes();
               try {
                   decode(ctx, replayable, out);

                   // Check if this handler was removed before continuing the loop.
                   // If it was removed, it is not safe to continue to operate on the buffer.
                   //
                   // See https://github.com/netty/netty/issues/1664
                   if (ctx.isRemoved()) {
                       break;
                   }

                   if (outSize == out.size()) {
                       if (oldInputLength == in.readableBytes() && oldState == state) {
                           throw new DecoderException(
                                   StringUtil.simpleClassName(getClass()) + ".decode() must consume the inbound " +
                                   "data or change its state if it did not decode anything.");
                       } else {
                           // Previous data has been discarded or caused state transition.
                           // Probably it is reading on.
                           continue;
                       }
                   }
               } catch (Signal replay) {
                   replay.expect(REPLAY);

                   // Check if this handler was removed before continuing the loop.
                   // If it was removed, it is not safe to continue to operate on the buffer.
                   //
                   // See https://github.com/netty/netty/issues/1664
                   if (ctx.isRemoved()) {
                       break;
                   }

                   // Return to the checkpoint (or oldPosition) and retry.
                   int checkpoint = this.checkpoint;
                   if (checkpoint >= 0) {
                       in.readerIndex(checkpoint);
                   } else {
                       // Called by cleanup() - no need to maintain the readerIndex
                       // anymore because the buffer has been released already.
                   }
                   break;
               }

               if (oldReaderIndex == in.readerIndex() && oldState == state) {
                   throw new DecoderException(
                          StringUtil.simpleClassName(getClass()) + ".decode() method must consume the inbound data " +
                          "or change its state if it decoded something.");
               }
               if (isSingleDecode()) {
                   break;
               }
           }
       } catch (DecoderException e) {
           throw e;
       } catch (Throwable cause) {
           throw new DecoderException(cause);
       }
   }
}

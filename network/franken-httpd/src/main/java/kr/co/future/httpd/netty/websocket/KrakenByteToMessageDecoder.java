package kr.co.future.httpd.netty.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;

import java.util.List;

public abstract class KrakenByteToMessageDecoder extends ByteToMessageDecoder{
	ByteBuf cumulation;
   private boolean singleDecode;
   private boolean decodeWasNull;
   private boolean first;

   protected KrakenByteToMessageDecoder() {
   	super();
   }

   /**
    * If set then only one message is decoded on each {@link #channelRead(ChannelHandlerContext, Object)}
    * call. This may be useful if you need to do some protocol upgrade and want to make sure nothing is mixed up.
    *
    * Default is {@code false} as this has performance impacts.
    */
   public void setSingleDecode(boolean singleDecode) {
       this.singleDecode = singleDecode;
   }

   /**
    * If {@code true} then only one message is decoded on each
    * {@link #channelRead(ChannelHandlerContext, Object)} call.
    *
    * Default is {@code false} as this has performance impacts.
    */
   public boolean isSingleDecode() {
       return singleDecode;
   }

   /**
    * Returns the actual number of readable bytes in the internal cumulative
    * buffer of this decoder. You usually do not need to rely on this value
    * to write a decoder. Use it only when you must use it at your own risk.
    * This method is a shortcut to {@link #internalBuffer() internalBuffer().readableBytes()}.
    */
   protected int actualReadableBytes() {
       return internalBuffer().readableBytes();
   }

   /**
    * Returns the internal cumulative buffer of this decoder. You usually
    * do not need to access the internal buffer directly to write a decoder.
    * Use it only when you must use it at your own risk.
    */
   protected ByteBuf internalBuffer() {
       if (cumulation != null) {
           return cumulation;
       } else {
           return Unpooled.EMPTY_BUFFER;
       }
   }

   /**
    * Gets called after the {@link ByteToMessageDecoder} was removed from the actual context and it doesn't handle
    * events anymore.
    */
   protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception { }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
       if (msg instanceof ByteBuf) {
           RecyclableArrayList out = RecyclableArrayList.newInstance();
           try {
               ByteBuf data = (ByteBuf) msg;
               first = cumulation == null;
               if (first) {
                   cumulation = data;
               } else {
                   if (cumulation.writerIndex() > cumulation.maxCapacity() - data.readableBytes()
                           || cumulation.refCnt() > 1) {
                       // Expand cumulation (by replace it) when either there is not more room in the buffer
                       // or if the refCnt is greater then 1 which may happen when the user use slice().retain() or
                       // duplicate().retain().
                       //
                       // See:
                       // - https://github.com/netty/netty/issues/2327
                       // - https://github.com/netty/netty/issues/1764
                       expandCumulation(ctx, data.readableBytes());
                   }
                   cumulation.writeBytes(data);
                   data.release();
                   
               }
               callDecode(ctx, cumulation, out);
           } catch (DecoderException e) {
               throw e;
           } catch (Throwable t) {
               throw new DecoderException(t);
           } finally {
               if (cumulation != null && !cumulation.isReadable()) {
                   cumulation.release();
                   cumulation = null;
               }
               int size = out.size();
               decodeWasNull = size == 0;

               for (int i = 0; i < size; i ++) {
                   ctx.fireChannelRead(out.get(i));
               }
               out.recycle();
           }
       } else {
           ctx.fireChannelRead(msg);
       }
   }

   private void expandCumulation(ChannelHandlerContext ctx, int readable) {
       ByteBuf oldCumulation = cumulation;
       cumulation = ctx.alloc().buffer(oldCumulation.readableBytes() + readable);
       cumulation.writeBytes(oldCumulation);
       oldCumulation.release();
   }

   @Override
   public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
       if (cumulation != null && !first && cumulation.refCnt() == 1) {
           // discard some bytes if possible to make more room in the
           // buffer but only if the refCnt == 1  as otherwise the user may have
           // used slice().retain() or duplicate().retain().
           //
           // See:
           // - https://github.com/netty/netty/issues/2327
           // - https://github.com/netty/netty/issues/1764
           cumulation.discardSomeReadBytes();
       }
       if (decodeWasNull) {
           decodeWasNull = false;
           if (!ctx.channel().config().isAutoRead()) {
               ctx.read();
           }
       }
       ctx.fireChannelReadComplete();
   }

   @Override
   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
       RecyclableArrayList out = RecyclableArrayList.newInstance();
       try {
           if (cumulation != null) {
               callDecode(ctx, cumulation, out);
               decodeLast(ctx, cumulation, out);
           } else {
               decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
           }
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

   /**
    * Called once data should be decoded from the given {@link ByteBuf}. This method will call
    * {@link #decode(ChannelHandlerContext, ByteBuf, List)} as long as decoding should take place.
    *
    * @param ctx           the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
    * @param in            the {@link ByteBuf} from which to read data
    * @param out           the {@link List} to which decoded messages should be added
    */
   protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
       try {
           while (in.isReadable()) {
               int outSize = out.size();
               int oldInputLength = in.readableBytes();
               decode(ctx, in, out);

               // Check if this handler was removed before continuing the loop.
               // If it was removed, it is not safe to continue to operate on the buffer.
               //
               // See https://github.com/netty/netty/issues/1664
               if (ctx.isRemoved()) {
                   break;
               }

               if (outSize == out.size()) {
                   if (oldInputLength == in.readableBytes()) {
                       break;
                   } else {
                       continue;
                   }
               }

               if (oldInputLength == in.readableBytes()) {
                   throw new DecoderException(
                           StringUtil.simpleClassName(getClass()) +
                           ".decode() did not read anything but decoded a message.");
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

   /**
    * Decode the from one {@link ByteBuf} to an other. This method will be called till either the input
    * {@link ByteBuf} has nothing to read when return from this method or till nothing was read from the input
    * {@link ByteBuf}.
    *
    * @param ctx           the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
    * @param in            the {@link ByteBuf} from which to read data
    * @param out           the {@link List} to which decoded messages should be added
    * @throws Exception    is thrown if an error accour
    */
   protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;

   /**
    * Is called one last time when the {@link ChannelHandlerContext} goes in-active. Which means the
    * {@link #channelInactive(ChannelHandlerContext)} was triggered.
    *
    * By default this will just call {@link #decode(ChannelHandlerContext, ByteBuf, List)} but sub-classes may
    * override this for some special cleanup operation.
    */
   protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
       decode(ctx, in, out);
   }
}

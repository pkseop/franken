package kr.co.future.httpd.netty.websocket;

import java.net.InetSocketAddress;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.util.CharsetUtil;
import static io.netty.buffer.ByteBufUtil.readBytes;

public class KrakenWebSocket00FrameDecoder extends KrakenReplayingDecoder<Void> implements WebSocketFrameDecoder {
	static final int DEFAULT_MAX_FRAME_SIZE = 30 * 1024 * 1024;

   private final long maxFrameSize;
   private boolean receivedClosingHandshake;
   
   private String host;
	private Channel channel;

   public KrakenWebSocket00FrameDecoder() {
       this(DEFAULT_MAX_FRAME_SIZE);
   }

   /**
    * Creates a new instance of {@code WebSocketFrameDecoder} with the specified {@code maxFrameSize}. If the client
    * sends a frame size larger than {@code maxFrameSize}, the channel will be closed.
    *
    * @param maxFrameSize
    *            the maximum frame size to decode
    */
   public KrakenWebSocket00FrameDecoder(int maxFrameSize) {
       this.maxFrameSize = maxFrameSize;
   }
   
   public KrakenWebSocket00FrameDecoder(int maxFrameSize, String host, Channel channel) {
		this.maxFrameSize = maxFrameSize;
		this.host = host;
		this.channel = channel;
	}

   @Override
   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
       // Discard all data received if closing handshake was received before.
       if (receivedClosingHandshake) {
           in.skipBytes(actualReadableBytes());
           return;
       }

       ByteBuf buf = null;
       // Decode a frame otherwise.
       byte type = in.readByte();
       if ((type & 0x80) == 0x80) {
           // If the MSB on type is set, decode the frame length
//           frame = decodeBinaryFrame(ctx, type, in);
           return;
       } else {
           // Decode a 0xff terminated UTF-8 string
      	 buf = decodeTextFrame(ctx, in);
       }

       if (buf != null) {
    	   try{
    		   kr.co.future.httpd.WebSocketFrame frame = new kr.co.future.httpd.WebSocketFrame();
    		   frame.setRemote((InetSocketAddress) channel.remoteAddress());
    		   frame.setHost(host);
    		   frame.setType(type);
    		   frame.setText(buf.toString(CharsetUtil.UTF_8));
    		   out.add(frame);
    	   } finally {
    		   buf.release();
    	   }
      } else {
      	throw KrakenReplayingDecoder.REPLAY;
      }
   }

//   private WebSocketFrame decodeBinaryFrame(ChannelHandlerContext ctx, byte type, ByteBuf buffer) {
//       long frameSize = 0;
//       int lengthFieldSize = 0;
//       byte b;
//       do {
//           b = buffer.readByte();
//           frameSize <<= 7;
//           frameSize |= b & 0x7f;
//           if (frameSize > maxFrameSize) {
//               throw new TooLongFrameException();
//           }
//           lengthFieldSize++;
//           if (lengthFieldSize > 8) {
//               // Perhaps a malicious peer?
//               throw new TooLongFrameException();
//           }
//       } while ((b & 0x80) == 0x80);
//
//       if (type == (byte) 0xFF && frameSize == 0) {
//           receivedClosingHandshake = true;
//           return new CloseWebSocketFrame();
//       }
//       ByteBuf payload = readBytes(ctx.alloc(), buffer, (int) frameSize);
//       return new BinaryWebSocketFrame(payload);
//   }

   private ByteBuf decodeTextFrame(ChannelHandlerContext ctx, ByteBuf buffer) {
       int ridx = buffer.readerIndex();
       int rbytes = actualReadableBytes();
       int delimPos = buffer.indexOf(ridx, ridx + rbytes, (byte) 0xFF);
       if (delimPos == -1) {
           // Frame delimiter (0xFF) not found
           if (rbytes > maxFrameSize) {
               // Frame length exceeded the maximum
               throw new TooLongFrameException();
           } else {
               // Wait until more data is received
               return null;
           }
       }

       int frameSize = delimPos - ridx;
       if (frameSize > maxFrameSize) {
           throw new TooLongFrameException();
       }

       ByteBuf binaryData = readBytes(ctx.alloc(), buffer, frameSize);
       buffer.skipBytes(1);

       int ffDelimPos = binaryData.indexOf(binaryData.readerIndex(), binaryData.writerIndex(), (byte) 0xFF);
       if (ffDelimPos >= 0) {
           binaryData.release();
           throw new IllegalArgumentException("a text frame should not contain 0xFF.");
       }

       return binaryData;
   }
}

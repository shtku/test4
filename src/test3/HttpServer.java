package test3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class HttpServer {
	public static void main(String[] args) throws IOException {
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(new InetSocketAddress(8080));
		ssc.configureBlocking(false);
		
		Selector selector = Selector.open();
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		while(true){
			if(selector.select(3000)==0){
				continue;
			}
			
			Iterator<SelectionKey> keyIter =  selector.selectedKeys().iterator();
			
			while(keyIter.hasNext()){
				SelectionKey key = keyIter.next();
				new Thread(new HttpHandler(key)).run();
				keyIter.remove();
			}
		}
		
	}
	
	private static class HttpHandler implements Runnable{
		private int bufferSize = 1024;
		private String localCharset = "UTF-8";
		private SelectionKey key;
		
		public HttpHandler(SelectionKey key){
			this.key = key;
		}
		
		public void handleAccept() throws IOException{
			SocketChannel sc = ((ServerSocketChannel)key.channel()).accept();
			sc.configureBlocking(false);
			sc.register(key.selector(), SelectionKey.OP_READ,ByteBuffer.allocate(bufferSize));
		}
		
		public void handleRead() throws IOException{
			SocketChannel sc = (SocketChannel) key.channel();
			ByteBuffer buffer = (ByteBuffer) key.attachment();
			buffer.clear();
			if(sc.read(buffer)==-1){
				sc.close();
			}else{
				buffer.flip();
				String receivedString = Charset.forName(localCharset).newDecoder().decode(buffer).toString();
				String[] requestMessage = receivedString.split("\r\n");
				for(String s:requestMessage){
					System.out.println(s);
					if(s.isEmpty()){
						break;
					}
				}
				
				String[] firstLine = requestMessage[0].split(" ");
				System.out.println();
				System.out.println("Method:\t" + firstLine[0]);
				System.out.println("url:\t" + firstLine[1]);
				System.out.println("HTTP Version:\t" + firstLine[2]);
				
				StringBuffer sendString = new StringBuffer();
				sendString.append("HTTP/1.1 200 OK\r\n");
				sendString.append("Content-type:text/html;Charset=" + localCharset + "\r\n");
				sendString.append("\r\n");
				sendString.append("<html><head><title>显示报文</title></head><body>");
				sendString.append("接受到的请求的报文是：<br>");
				for(String s:requestMessage){
					sendString.append(s+"<br>");
				}
				sendString.append("</body></html>");
				
				buffer = ByteBuffer.wrap(sendString.toString().getBytes(localCharset));
				sc.write(buffer);
				sc.close();
			}
		}
		public void run() {
			try{
				if(key.isAcceptable()){
					handleAccept();
				}
				if(key.isReadable()){
					handleRead();
				}
			}catch(IOException e){
				e.printStackTrace();
			}
			
		}
		
	}
}

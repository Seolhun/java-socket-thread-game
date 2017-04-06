package game.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.StringTokenizer;

import game.common.protocol.Protocol;

public class ChatServer {
	private static final int PORT = 9001;

	/**
	 * 방에 있는 유저 이름을 정하는 것, HashSet을 통해 중복해결.
	 */
	private static HashSet<String> names = new HashSet<String>();

	/**
	 * 모든 채터를 다른 채터들에게 프린트 하는 것. 이것은 쉽게 broadcast 메세지 한다.
	 */
	private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

	/**
	 * 포트에서 수신 대기하고 고객의 요청을 처리하는 핸들러 스레드를 생성하는 메인 메소드입니다.
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("채팅 서버가 작동되었습니다.");
		ServerSocket listener = new ServerSocket(PORT);
		try {
			while (true) {
				new ProtocolHandler(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	/**
	 * 핸들러 스레드이며, 고객의 요청을 처리합니다.
	 */
	private static class ProtocolHandler extends Thread {
		private String name;
		private Socket socket;
		private BufferedReader reader;
		private PrintWriter poster;
		private String msg;
		
		public ProtocolHandler(Socket socket) {
			this.socket = socket;
		}

		/**
		 * 소켓 통신의 핵심이며, potser에서 프로토콜을 보낸다.
		 */
		public void run() {
			try {
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				poster = new PrintWriter(socket.getOutputStream(), true);
				System.out.println("return : 서버가 통신 준비를 완료하였습니다.");
				
				/*
				 * 현재 요청된 유저에게 이름을 요청한다. 이미 있으면, 다시 요청하고, 없으면 추가하고 끝낸다.
				 */
				while (true) {
//					System.out.println("param : getReceiveBufferSize() : "+socket.getReceiveBufferSize());
//					System.out.println("param : getSendBufferSize() : "+socket.getSendBufferSize());
//					System.out.println("param : getInetAddress() : "+socket.getInetAddress());
//					System.out.println("param : getRemoteSocketAddress() : "+socket.getRemoteSocketAddress());
//					System.out.println("param : getLocalSocketAddress() : "+socket.getLocalSocketAddress());
//					System.out.println("param : getLocalAddress() : "+socket.getLocalAddress());
					
					// 클라이언트에서 오는 요청을 분석한다.
					String line = reader.readLine();
					System.out.println("return : 서버가 유저에게 받은 요청 "+line);
					StringTokenizer st = new StringTokenizer(line, "|");
					int protocol = Integer.parseInt(st.nextToken());
					
					// 맞는 프로토콜을 찾는다.
					switch (protocol) {
					case Protocol.SUBMITNAME:
						name = st.nextToken();
						if (name == null) {
							System.out.println("return : 서버가 유저에게 받은 이름 "+name+"가 중복으로 실패하였습니다.");
							msg=setProtocol(msg, protocol);
							poster.println(msg);
							return;
						}
						synchronized (names) {
							if (!names.contains(name)) {
								names.add(name);
							}
						}
						
						// 이름이 승인되었다는 메세지를 보낸다.
						msg=setProtocol(msg, Protocol.NAMEACCEPTED);
						poster.println(msg);
						System.out.println("return : 서버가 유저에게 받은 "+name+"이 성공적으로 추가되었습니다.");
						break;
						
					case Protocol.NAMEACCEPTED:
						writers.add(poster);
						break;
					case Protocol.MESSAGE:
						// 이름이 현재 성공적으로 선택되었으면, 메세지를 받을 수 있게 소켓을 추가하여준다.
						String messagePoster=st.nextToken();
						String content=st.nextToken();
						if (content == null || content.equals("")) {
							break;
						}
						
						for (PrintWriter writer : writers) {
							msg=ChatServer.setMessage(msg, protocol, messagePoster+"|"+content);
							writer.println(msg);
						}
						break;
					case Protocol.READY:
						String writer=st.nextToken();
						break;
//					case Protocol.ALLREADY:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
//					case Protocol.START:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;	
//					case Protocol.SUBMIT:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
//					case Protocol.TIME:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;	
//					case Protocol.TIMEOUT:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
//					case Protocol.ANSWER:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
//					case Protocol.ITEM:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
//					case Protocol.USEITEM:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
//					case Protocol.HINT:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
//					case Protocol.RESULT:
//						String writer=st.nextToken();
//						String content=st.nextToken();
//						messageArea.append(writer+" : "+content + "\n");
//						break;
					default:
						break;
					}
				}
			} catch (IOException e) {
				System.out.println(e);
			} finally {
				// This client is going down! Remove its name and its print
				// writer from the sets, and close its socket.
				if (name != null) {
					names.remove(name);
				}
				if (poster != null) {
					writers.remove(poster);
				}
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static String setProtocol(String msg, int protocol) {
		msg = String.valueOf(protocol) + "|";
		return msg;
	}

	public static String setMessage(String msg, int protocol, String attatch) {
		msg = String.valueOf(protocol) + "|" + attatch + "|";
		return msg;
	}
}
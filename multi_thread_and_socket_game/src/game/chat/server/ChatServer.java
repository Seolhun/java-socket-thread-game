package game.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import game.common.domain.User;
import game.common.protocol.Protocol;

public class ChatServer {
	private static final int PORT = 9001;

	// 방에 있는 유저 이름을 정하는 것, HashSet을 통해 중복해결.
	private static HashSet<String> names = new HashSet<String>();
	
	private static ArrayList<User> users = new ArrayList<>();

	// 모든 채터를 다른 채터들에게 프린트 하는 것. 이것은 쉽게 broadcast 메세지 한다.
	private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

	// 포트에서 수신 대기하고 고객의 요청을 처리하는 핸들러 스레드를 생성하는 메인 메소드입니다.
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

	// 핸들러 스레드이며, 고객의 요청을 처리합니다.
	private static class ProtocolHandler extends Thread {
		private String name;
		private String question;
		private User user;
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
					// 클라이언트에서 오는 요청을 분석한다.
					String line = reader.readLine();
					System.out.println("return : 서버가 유저에게 받은 요청 " + line);
					StringTokenizer st = new StringTokenizer(line, "|");
					int protocol = Integer.parseInt(st.nextToken());
					String writer = "";

					// 맞는 프로토콜을 찾는다.
					switch (protocol) {
					
					//클라이언트 이름 수신, 가능여부 송신.
					case Protocol.SUBMITNAME:
						name = st.nextToken();
						if (name == null) {
							postProtocol(msg, protocol, poster);
							return;
						}
						
						synchronized (names) {
							if (!names.contains(name)) {
								//Default Value
								names.add(name);
							} else {
								System.out.println("return : 서버가 유저에게 받은 이름 " + name + "가 중복으로 실패하였습니다.");								
								postProtocol(msg, protocol, poster);
								return;
							}
						}
						
						synchronized (users) {
							user=new User(name, 0, "N", "N", "");
							users.add(user);
						}


						// 이름이 승인되었다는 메세지 송신.
						postProtocol(msg, Protocol.NAMEACCEPTED, poster);
						System.out.println("return : 서버가 유저에게 받은 " + name + "이 성공적으로 추가되었습니다.");
						
						writers.add(poster);
						for (PrintWriter pr : writers) {
							if (pr != poster) {
								postMessage(msg, Protocol.NEWIN, name, pr);
							} else {
								for (User user2 : users) {
									postMessage(msg, Protocol.NEWIN, user2.getName(), pr);
								}
							}
						}
						break;
						
					//메세지를 받으면 모든 유저에게 송신.
					case Protocol.MESSAGE:
						// 이름이 현재 성공적으로 선택되었으면, 메세지를 받을 수 있게 소켓을 추가하여준다.
						String messagePoster = st.nextToken();
						String content = st.nextToken();
						if (content == null || content.equals("")) {
							break;
						}

						postMessageToAll(msg, protocol, messagePoster + "|" + content);
						break;
						
					//준비되었다는 메세지와 모든 유저의 준비여부 체크 
					case Protocol.READY:
						writer = st.nextToken();
						
						//방에 있는 사람 행동 체크여부.
						boolean allReady = true;
						postMessageToAll(msg, protocol, writer);
						
						for (User user2 : users) {
							if (user2.getName().equals(writer)) {
								user2.setReady("Y");
							}
							
							// 모두가 레디상태인지 확인.
							if (user2.getReady().equals("N")) {
								allReady = false;
								break;
							}
						}

						if (allReady) {
							content = "모든 사용자가 레디를 완료하였습니다.";
							postMessageToAll(msg, Protocol.ALLREADY, content);
						} else {
							break;
						}
						
						content = "3초후 시작합니다.";
						postMessageToAll(msg, Protocol.NOTICE, content);
						//다른기능 이후 시행되기 위함.
						try {
							Thread.sleep(3000);			
						} catch (InterruptedException e) {
							// restore interrupted status
							Thread.currentThread().interrupt(); 
						}
						
						content = "시작했습니다. 1000점을 먼저 달성하는 사람이 이기는 게임입니다.";
						postMessageToAll(msg, Protocol.NOTICE, content);
						
						//첫 문제 만들기.
						sendQuestion(msg, question, content);
						break;
					
					//유저 준비 취소
					case Protocol.READYCANCEL:
						writer = st.nextToken();

						// 방에 있는 사람 레디 여부 체크 - 레디 취소했으면 N
						for (User user2 : users) {
							if (user2.getName().equals(writer)) {
								user2.setReady("N");
							}
						}
						postMessageToAll(msg, protocol, writer);
						break;
						
					//다음 문제 제출.
					case Protocol.QUESTION:
						//문제 만들기.
						question=String.valueOf(Math.round((Math.random()*100+1)));
						content="0~100사이의 숫자를 입력하세요.";
						postMessageToAll(msg, Protocol.QUESTION, content);
					 break;
					 
					 //제출여부 체크.
					case Protocol.SUBMIT:
						writer = st.nextToken();
						String answer = st.nextToken();
						System.out.println("return : "+writer+"가 "+answer+"를 제출했습니다.");
						
						//모든 유저가 제출했는지 체크.
						content = "모든 유저가 제출하였습니다.";
						if(checkAllUserDid(msg, writer, answer, content)){
							//정답체크 로직							
							checkAnswer(msg, answer);
							
							//제출한 흔적 초기화 = submit Y -> N으로
							resetUserDid();
						} else {
							break;
						}

						try {
							Thread.sleep(2000);
							content="2초후에 다시 시작합니다.";
							postMessageToAll(msg, Protocol.NOTICE, content);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						sendQuestion(msg, question, content);
					 break;
					// case Protocol.TIME:
					// String writer=st.nextToken();
					// String content=st.nextToken();
					// messageArea.append(writer+" : "+content + "\n");
					// break;
					// case Protocol.TIMEOUT:
					// String writer=st.nextToken();
					// String content=st.nextToken();
					// messageArea.append(writer+" : "+content + "\n");
					// break;
					// case Protocol.ANSWER:
					// String writer=st.nextToken();
					// String content=st.nextToken();
					// messageArea.append(writer+" : "+content + "\n");
					// break;
					// case Protocol.ITEM:
					// String writer=st.nextToken();
					// String content=st.nextToken();
					// messageArea.append(writer+" : "+content + "\n");
					// break;
					// case Protocol.USEITEM:
					// String writer=st.nextToken();
					// String content=st.nextToken();
					// messageArea.append(writer+" : "+content + "\n");
					// break;
					// case Protocol.HINT:
					// String writer=st.nextToken();
					// String content=st.nextToken();
					// messageArea.append(writer+" : "+content + "\n");
					// break;
					// case Protocol.RESULT:
					// String writer=st.nextToken();
					// String content=st.nextToken();
					// messageArea.append(writer+" : "+content + "\n");
					// break;
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

	// 프로토콜 세팅
	public static void postProtocol(String msg, int protocol, PrintWriter poster) {
		msg = String.valueOf(protocol) + "|";
		poster.println(msg);
	}

	// 프로토콜 세팅 후 메세지 세팅하기.
	public static void postMessage(String msg, int protocol, String content, PrintWriter poster) {
		msg = String.valueOf(protocol) + "|" + content + "|";
		poster.println(msg);
	}

	// 모든 사람에게 메세지 보내기.
	public static void postMessageToAll(String msg, int protocol, String content) {
		msg = String.valueOf(protocol) + "|" + content + "|";
		for (PrintWriter writer : writers) {
			writer.println(msg);
		}
	}
	
	//writer(제출자)의 제출여부 Y로 바꾼 후, 모든 유저가 제출했을 시, Content로 메세지를 전달한다.
	public static boolean checkAllUserDid(String msg, String writer, String answer, String content){
		boolean allDid=false;
		for (User user2 : users) {
			
			//writer제출자와 나머지 유저의 상태 체크.
			if (user2.getName().equals(writer)) {
				user2.setAnswer(answer);
				user2.setSubmit("Y");
			}
			
			System.out.println("param : name "+user2.getName());
			System.out.println("param : submet "+user2.getSubmit());
			System.out.println("param : writer "+writer);
			
			// 모두가 제출상태인지 확인.
			if (user2.getSubmit().equals("N")) {
				allDid = false;
				return allDid;
			}
			
			allDid=true;
		}

		if (allDid) {
			postMessageToAll(msg, Protocol.NOTICE, content);
		}
		return allDid;
	}
	
	//모든 유저가 제출한 흔적 리셋
	public static void resetUserDid(){
		for (User user2 : users) {
			user2.setSubmit("N");
		}
	}
	
	//점수 체크와 점수부여.
	public static void checkAnswer(String msg, String question){
		int ques=Integer.parseInt(question);
		for (User user2 : users) {
			int ans=Integer.parseInt(user2.getAnswer());
			if(ques==ans){
				int item=user2.getItem();
				item+=1;
				user2.setItem(item);
			}
			int point=Math.abs(ques-ans);
			postMessageToAll(msg, Protocol.NOTICE, user2.getName()+"님이 "+String.valueOf(point)+" 점수를 얻었습니다.");
			point+=user2.getPoint();
			user2.setPoint(point);
		}
		
		for (int i = 0; i < users.size(); i++) {
			for (int j = 0; j < users.size()-i;) {
				int previousPoint=users.get(i).getPoint();
				int nextPoint=users.get(j).getPoint();
				User tempUser=new User();
				if(previousPoint>nextPoint){
					tempUser=users.get(j);
					users.set(j, users.get(i));
					users.set(i, tempUser);
				} 
			}
		}
		
		for (int i = 0; i < users.size(); i++) {
			postMessageToAll(msg, Protocol.NOTICE, users.get(i).getName()+"님이 현재 "+String.valueOf(users.get(i).getPoint())+"로 "+String.valueOf((i+1))+"등 입니다.");	
		}
	}
	
	public static void sendQuestion(String msg, String question, String content){
		question=String.valueOf(Math.round((Math.random()*100+1)));
		content="0~100사이의 숫자를 입력하세요.";
		postMessageToAll(msg, Protocol.QUESTION, content);
	}
}
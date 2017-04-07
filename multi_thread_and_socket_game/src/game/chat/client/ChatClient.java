package game.chat.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import game.chat.server.ChatServer;
import game.common.protocol.Protocol;

public class ChatClient implements ActionListener {
	private BufferedReader reader;
	private PrintWriter poster;
	private JFrame frame = new JFrame("채팅창");
	private JTextField textField = new JTextField(40);
	private JTextArea messageArea = new JTextArea(30, 40);
	private JTextArea messageArea2 = new JTextArea(30, 40);
	private JTextArea messageArea3 = new JTextArea(10, 20);

	private Socket socket;
	private String myName;
	private String msg;
	private JButton b1;

	// 클라이언트 실행.
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}

	// 채팅방 레이아웃
	public ChatClient() {

		// Layout GUI
		textField.setEditable(false);
		messageArea.setEditable(false);
		frame.getContentPane().add(textField, BorderLayout.NORTH);
		frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);

		// panel1 - Function LayOut
		JPanel p1 = new JPanel();
		p1.setLayout(new GridLayout(1, 2));
		b1 = new JButton("준비하기");
		b1.addActionListener(this);
		p1.add(b1);

		messageArea2.setBackground(new Color(255, 255, 100));
		frame.getContentPane().add(p1, BorderLayout.SOUTH);
		frame.getContentPane().add(new JScrollPane(messageArea2), BorderLayout.EAST);
		frame.setLocation(400, 50);

		messageArea3.setBackground(new Color(255, 255, 220));
		frame.getContentPane().add(new JScrollPane(messageArea3), BorderLayout.WEST);
		frame.pack();
		
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text=textField.getText();
				if(text.length()<1){
					return;
				}
				ChatServer.postMessage(msg, Protocol.MESSAGE, myName + "|" + text, poster);
				textField.setText("");
			}
		});
	}

	// Chat Client 실행.
	private void run() throws IOException {
		String serverAddress = getServerAddress();
		if (serverAddress == null) {
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
			return;
		}
		socket = new Socket(serverAddress, 9001);
		System.out.println("return : 서버에 연결되었습니다. : " + serverAddress);

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		poster = new PrintWriter(socket.getOutputStream(), true);
		System.out.println("return : 서버와 통신 준비가 완료되었습니다.");

		System.out.println("return : 채팅방에 사용 할 이름을 제출하세요.");
		String name = getName();
		ChatServer.postMessage(msg, Protocol.SUBMITNAME, name, poster);

		while (true) {
			String line = reader.readLine();
			int protocol = 0;
			StringTokenizer st = new StringTokenizer(line, "|");
			if (line.length() > 0) {
				System.out.println("return : 유저가 서버에서 받은 요청 " + line);
				// 서버에서오는 요청을 분석한다.
				protocol = Integer.parseInt(st.nextToken());
			}
			String writer = "";
			String content = "";

			switch (protocol) {
			//채팅방에서 쓸 이름 제출.
			case Protocol.SUBMITNAME:
				System.out.println("return : 채팅방에 사용 할 이름을 다시 제출하세요.");
				ChatServer.postMessage(msg, protocol, getName(), poster);
				break;
				
			//이름 중복 체크 문제 없음.
			case Protocol.NAMEACCEPTED:
				textField.setEditable(true);
				System.out.println("return : 채팅창이 수락되었습니다.");
				break;
				
			//새로운 사람 접속시.
			case Protocol.NEWIN:
				content = st.nextToken();
				messageArea3.append(content + "\n");
				break;
				
			//채팅 메세지.
			case Protocol.MESSAGE:
				writer = st.nextToken();
				content = st.nextToken();
				messageArea.append(writer + " : " + content + "\n");
				break;
				
			//공지
			case Protocol.NOTICE:
				content = st.nextToken();
				messageArea2.append(content + "\n");
				break;
				
			//준비 
			case Protocol.READY:
				writer = st.nextToken();
				// 서버와 통신은 ActionListener에서 처리했음.
				content = writer + "님이 준비했습니다.";
				messageArea2.append(content + "\n");
				break;
				
			// 준비 취소.
			case Protocol.READYCANCEL:
				writer = st.nextToken();
				// 서버와 통신은 ActionListener에서 처리했음.
				content = writer + "님이 준비를 취소하셨습니다.";
				messageArea2.append(content + "\n");
				break;
				
			//모든 사용자 준비 시 게임시작되는 것.
			case Protocol.ALLREADY:
				content = st.nextToken();
				b1.setEnabled(false);
				messageArea2.append(content + "\n");
				break;
				
			//서버에서 문제 종료 후 답을 알려주는 것.
			case Protocol.ANSWER:
				break;
				
			//서버에서 문제가 오면, 답장을 내는 것.
			case Protocol.QUESTION:
				String answer= JOptionPane.showInputDialog(frame, "정답을 입력하세요. : ", "정답", JOptionPane.PLAIN_MESSAGE);
				ChatServer.postMessage(msg, Protocol.SUBMIT, myName+"|"+answer, poster);
				break;
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
			}
		}
	}

	private String getServerAddress() {
		String address = "127.0.0.1"; 
//		String address =JOptionPane.showInputDialog(frame, "서버 아이피를 입력하세요. : ", "채팅창에 오신것을 환영합니다.", JOptionPane.QUESTION_MESSAGE);
//		if (address == null) {
//			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
//		}
		return address;
	}

	private String getName() {
		String name = JOptionPane.showInputDialog(frame, "닉네임을 정하세요. : ", "닉네임", JOptionPane.PLAIN_MESSAGE);
		if (name == null) {
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		}
		myName = name;
		return name;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//준비하기 버튼 클릭.
		if (e.getSource() == b1) {
			if (b1.getText().equals("취소하기")) {
				b1.setText("준비하기");
				ChatServer.postMessage(msg, Protocol.READYCANCEL, myName, poster);
			} else {
				ChatServer.postMessage(msg, Protocol.READY, myName, poster);
				b1.setText("취소하기");
			}

		} 
	}

}
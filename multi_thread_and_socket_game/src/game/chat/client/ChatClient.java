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


public class ChatClient implements ActionListener{
    private BufferedReader reader;
    private PrintWriter poster;
    private JFrame frame = new JFrame("채팅창");
    private JTextField textField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(30, 40);
    private JTextArea messageArea2 = new JTextArea(30, 40);
    
    private Socket socket;
    private String myName;
    private String msg;
    private JButton b1, b2;
    
    //클라이언트 실행.
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }

    //채팅방 레이아웃
    public ChatClient() {
    	
        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        
        //panel1 - Function LayOut
        JPanel p1=new JPanel();
        p1.setLayout(new GridLayout(1, 2));
        b1 = new JButton("준비하기");
        b2 = new JButton("제출하기");
        b1.addActionListener(this);
        b2.addActionListener(this);
        p1.add(b1);
		p1.add(b2);
		frame.getContentPane().add(p1, BorderLayout.SOUTH);	
		
		messageArea2.setBackground(new Color(182, 255, 0, 50));
		frame.getContentPane().add(new JScrollPane(messageArea2), BorderLayout.EAST);
		frame.setLocation(400, 50);		
		frame.pack();      
		
        // Add Listeners
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	msg=ChatServer.setMessage(msg, Protocol.MESSAGE, myName+"|"+textField.getText());
                poster.println(msg);
                textField.setText("");
            }
        });
    }
    
    //Chat Client 실행.
    private void run() throws IOException {
        String serverAddress = getServerAddress();
        if(serverAddress==null){
        	frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        	return;
        }
        socket = new Socket(serverAddress, 9001);
        System.out.println("return : 서버에 연결되었습니다. : "+serverAddress);
        
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        poster = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("return : 서버와 통신 준비가 완료되었습니다.");

        System.out.println("return : 채팅방에 사용 할 이름을 제출하세요.");
        String name=getName();
		msg=ChatServer.setMessage(msg, Protocol.SUBMITNAME, name);
		poster.println(msg);
        
        while (true) {
//			System.out.println("param : getReceiveBufferSize() : "+socket.getReceiveBufferSize());
//			System.out.println("param : getSendBufferSize() : "+socket.getSendBufferSize());
//			System.out.println("param : getInetAddress() : "+socket.getInetAddress());
//			System.out.println("param : getRemoteSocketAddress() : "+socket.getRemoteSocketAddress());
//			System.out.println("param : getLocalSocketAddress() : "+socket.getLocalSocketAddress());
//			System.out.println("param : getLocalAddress() : "+socket.getLocalAddress());
        	
        	String line = reader.readLine();
        	int protocol=0;
        	StringTokenizer st = new StringTokenizer(line, "|");
        	if(line.length()>0){
        		System.out.println("return : 유저가 서버에서 받은 요청 "+line);
                // 서버에서오는 요청을 분석한다.
     			protocol = Integer.parseInt(st.nextToken());	
        	}
        	String writer="";
			String content="";
        	
			switch (protocol) {
			case Protocol.SUBMITNAME:
		        System.out.println("return : 채팅방에 사용 할 이름을 다시 제출하세요.");
				msg=ChatServer.setMessage(msg, protocol, getName());
				poster.println(msg);
				break;
			case Protocol.NAMEACCEPTED:
				msg=ChatServer.setProtocol(msg, protocol);
				poster.println(msg);
				textField.setEditable(true);
				System.out.println("return : 채팅창이 수락되었습니다.");
				break;
			case Protocol.MESSAGE:
				writer=st.nextToken();
				content=st.nextToken();
				messageArea.append(writer+" : "+content + "\n");
				break;
//			case Protocol.READY:
//				writer=st.nextToken();
//				content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.ALLREADY:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.START:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;	
//			case Protocol.SUBMIT:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.TIME:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;	
//			case Protocol.TIMEOUT:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.ANSWER:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.ITEM:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.USEITEM:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.HINT:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
//			case Protocol.RESULT:
//				String writer=st.nextToken();
//				String content=st.nextToken();
//				messageArea.append(writer+" : "+content + "\n");
//				break;
			}
        }
    }
    
    private String getServerAddress() {
        String address=JOptionPane.showInputDialog(frame, "서버 아이피를 입력하세요. : ", "채팅창에 오신것을 환영합니다.", JOptionPane.QUESTION_MESSAGE);
        if(address==null){
        	frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
        return address;
    }

    private String getName() {
    	String name=JOptionPane.showInputDialog(frame, "닉네임을 정하세요. : ", "닉네임", JOptionPane.PLAIN_MESSAGE);
    	if(name==null){
        	frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        }
    	myName=name;
        return name;
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==b1){
			System.out.println("param : 준비하기 버튼 클릭");
			if(b1.getText().equals("취소하기")){
				b1.setText("준비하기");
			} else {
				b1.setText("취소하기");
			}
						
			
		} else if(e.getSource()==b2){
			System.out.println("param : 제출하기 버튼 클릭");
		}
	}
    
    
}
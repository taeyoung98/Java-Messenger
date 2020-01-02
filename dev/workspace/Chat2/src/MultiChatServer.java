import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.google.gson.Gson;

public class MultiChatServer extends JFrame implements ActionListener
{
	private JPanel contentPane;
	private JTextArea s_msgOut= new JTextArea();
	private JButton startButton = new JButton("서버 실행");
	private JButton stopButton = new JButton("서버 중지");
	
	private ServerSocket ss = null;
	private Socket s= null;
	
	ArrayList<ChatThread> chatThreads = new ArrayList<ChatThread>();
	
	Logger logger;
	
	ArrayList<String> userList = new ArrayList<String>();
	HashSet hs = new HashSet();
	
	MultiChatServer()  // 생성자 
	{
		super("모든걸 볼수있는 서버야");
		init();  // 화면 구성 메소드
		buttons();  // 액션리스너 설정 메소드
	}
	
	private void init() 
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 321, 364);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane(s_msgOut, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBounds(12, 10, 281, 256);
		contentPane.add(scrollPane);
		
		scrollPane.setViewportView(s_msgOut);
		s_msgOut.setFont(new Font("a시네마M", Font.PLAIN, 13));
		s_msgOut.setForeground(Color.WHITE);
		s_msgOut.setBackground(new Color(153, 153, 102));
		s_msgOut.append(" ~ 사용자 접속 무한 대기중 ~ \n");
		
		startButton.setBounds(22, 279, 115, 33);
		startButton.setFont(new Font("a시네마M", Font.PLAIN, 14));
		contentPane.add(startButton);
		
		stopButton.setBounds(165, 279, 115, 33);
		stopButton.setFont(new Font("a시네마M", Font.PLAIN, 14));
		contentPane.add(stopButton);
		
		this.setVisible(true);  // true로 해야 화면에 보임.
	}
	
	private void buttons()
	{
		startButton.addActionListener(this);
		stopButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object obj = e.getSource();
		
		if(obj == startButton) {
			System.out.println("서버 실행 버튼 클릭");
			start();  // 소켓 생성 및 사용자 접속 대기
		}
		else if(obj == stopButton) {
			System.out.println("서버 중지 버튼 클릭");
			//v.System.exit(0);
			System.exit(0);
		}
	}
	
	public void start()
	{
		logger = Logger.getLogger(this.getClass().getName());
		
		try {
			ss = new ServerSocket(12345);  // 5111 포트 사용
			logger.info("MultiChatServer start");
			
			while (true) {
				s = ss.accept();
				s_msgOut.append(" ~ 사용자 접속 무한 대기중 ~ \n");
				
				ChatThread chat = new ChatThread();
				chatThreads.add(chat);
				chat.start();
			}
		}
		catch (Exception e) {
			logger.info("[MultiChatServer]start() Exception 발생!!");
			e.printStackTrace();
		}
	}
	
	// 연결된 모든 클라이언트에 메시지 중계
	void msgSendAll(String msg)
	{
		for(ChatThread ct : chatThreads) {
			ct.outMsg.println(msg);
		}
	}

	// 각각의 클라이언트 관리를 위한 쓰레드 클래스
	class ChatThread extends Thread //1개의 스레드에서는 1개의 일만 처리할 수 있다.
	{
		// 수신 메시지 및 파싱 메시지 처리를 위한 변수 선언
				String msg;

		        // 메시지 객체 생성
				Message m = new Message();

		        // Json Parser 초기화
				Gson gson = new Gson();
				//Sample Message {"id":"user1","passwd":"1234","msg":"hahaha","type":"msg"};

				// 입출력 스트림
				private BufferedReader inMsg = null;
				private PrintWriter outMsg = null;
		
		public void run()  // 스레드에서 처리할 일을 기재한다
		{
			boolean status = true;
			logger.info("ChatThread start...");
			
			try
			{
				// 입출력 스트림 생성
				inMsg = new BufferedReader(new InputStreamReader(s.getInputStream()));
				outMsg = new PrintWriter(s.getOutputStream(),true);
				
				// 상태정보가 true 이면 루프를 돌면서 사용자로 부터 수신된 메시지 처리
				
				while(status) {
					// 수신된 메시지를 msg 변수에 저장
					msg = inMsg.readLine();
					
					// JSON 메시지를 Message 객체로 매핑
					m = gson.fromJson(msg,Message.class);
												
					// 파싱된 문자열 배열의 두번째 요소 값에 따라 처리
					// 로그아웃 메시지 인 경우
					if(m.getType().equals("logout")) {
						userList.remove(m.getId());
						hs.remove(m.getId());
						chatThreads.remove(this);
						
						
						String user = "";
						int people = 0;
						for(int i=0; i<userList.size(); i++) {
							user = user+userList.get(i)+"\n";
						}
						people = userList.size();
						
						s_msgOut.append("사용자 "+m.getId()+" 로그아웃했습니다.\n");
						msgSendAll(gson.toJson(new Message(m.getId(),"","님이 종료했습니다.","server", user, people)));
						// 해당 클라이언트 스레드 종료로 인해 status 를 false 로 설정
						status = false;
					}
					
					// 로그인 메시지 인 경우
					else if(m.getType().equals("login"))
					{
						userList.add(m.getId());
						hs.addAll(userList);
						userList.clear();
						userList.addAll(hs);
						
						
						String user = "";
						int people = 0;
						for(int i=0; i<userList.size(); i++) {
							user += userList.get(i)+"\n";
						}
						people  = userList.size();
						
						s_msgOut.append("사용자 "+m.getId()+" 로그인했습니다.\n");
						msgSendAll(gson.toJson(new Message(m.getId(),"","님이 로그인했습니다.","server", user, people)));
					}
					// 그밖의 경우 즉 일반 메시지인 경우
					else
					{
						msgSendAll(msg);
						s_msgOut.append(m.getId()+": "+m.getMsg()+"\n");
						System.out.println(m.getType());
					}
				}
				// 루프를 벗어나면 클라이언트 연결 종료 이므로 스레드 인터럽트
				this.interrupt();
				logger.info(this.getName() + " 종료됨!!");
			}
			catch (Exception e)
			{
				chatThreads.remove(this);
				logger.info("[ChatThread]run() IOException 발생!!");
                e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args)
	{
		MultiChatServer server = new MultiChatServer();
		server.start();
	}
	
}

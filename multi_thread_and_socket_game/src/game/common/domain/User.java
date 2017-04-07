package game.common.domain;

public class User {
	// PK Value
	private String name;
	private int point;
	private String ready;
	private String submit;
	private String answer;
	private int item;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPoint() {
		return point;
	}

	public void setPoint(int point) {
		this.point = point;
	}

	public String getSubmit() {
		return submit;
	}

	public void setSubmit(String submit) {
		this.submit = submit;
	}

	public String getReady() {
		return ready;
	}

	public void setReady(String ready) {
		this.ready = ready;
	}
	
	public int getItem() {
		return item;
	}

	public void setItem(int item) {
		this.item = item;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}
	
	public User(){
		
	}
	
	public User(String name, int point, String ready, String submit, String answer) {
		this.name = name;
		this.point = point;
		this.ready = ready;
		this.submit=submit;
		this.answer=answer;
	}
	
	public User(String name, int point, String ready) {
		this.name = name;
		this.point = point;
		this.ready = ready;
	}
}

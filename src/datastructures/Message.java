package datastructures;

public class Message implements Content {
	
	private final MessageType type;
	private final Content content;
	private final int sender;
	
	public Message(MessageType type, Content content, int sender) {
		this.type = type;
		this.content = content;
		this.sender = sender;
	}
	
	public MessageType getMessageType() {
		return type;
	}
	
	public Content getContent() {
		//TODO solve the problem of the type casting because content can be a message or block
		//easiest way is to have two different fields
		return content;
	}
	
	public int getSender() {
		return sender;
	}
}

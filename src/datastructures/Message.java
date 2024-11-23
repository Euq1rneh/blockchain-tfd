package datastructures;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Message implements Serializable{
	
	private static final long serialVersionUID = -1241629326055537554L;
	
	private final MessageType type;
	private final Block block;
	private final Message message;
	private final int sender;
	
	private List<Block> blockchain;
	private List<Block> notarizechain;
	private int epochNumber;
	
	
	public Message(MessageType type, int sender, Message message, Block block) {
		this.type = type;
		this.sender = sender;
		this.message = message;
		this.block = block;
	}
	
	public Message(MessageType type, int sender) {
		this.block = null;
		this.message = null;

		this.type = type;
		this.sender = sender;
	}
	
	public Message(int sender, List<Block> blockchain, List<Block> notarizechain, int epochNumber, MessageType type) {
		this.block = null;
		this.message = null;

		this.type = type;
		this.sender = sender;
		this.blockchain = blockchain;
		this.notarizechain = notarizechain;
		this.epochNumber = epochNumber;
		
	}
	
	public MessageType getMessageType() {
		return type;
	}
	
	public Message getMessage() {
		return message;
	}
	
	public Block getBlock() {
		return block;
	}
	
	public int getSender() {
		return sender;
	}
	
	public List<Block> getBlockchain() {
		return blockchain;
	}

	public List<Block> getNotarizechain() {
		return notarizechain;
	}

	public int getEpochNumber() {
		return epochNumber;
	}

	@Override
	public int hashCode() {
		return Objects.hash(block, message, sender, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		return Objects.equals(block, other.block) && Objects.equals(message, other.message) && sender == other.sender
				&& type == other.type;
	}


	
	
}

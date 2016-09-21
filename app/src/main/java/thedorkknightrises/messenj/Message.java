package thedorkknightrises.messenj;

import java.io.Serializable;

/**
 * Created by Samriddha Basu on 9/21/2016.
 */
public class Message implements Serializable {
    public static final byte TYPE_DISCONNECT = 0;
    public static final byte TYPE_ANNOUNCE = 1;
    public static final byte TYPE_TEXT = 2;
    public static final byte TYPE_FILE = 3;
    public static final byte TYPE_CONNECT = 4;
    private byte type;
    private byte[] data;
    private String text;
    private String sender;
    private int number;

    public Message(String text) {
        this.type = TYPE_ANNOUNCE;
        this.text = text;
    }

    public Message(byte type, String text, String sender) {
        this.type = type;
        this.text = text;
        this.sender = sender;
    }

    public Message(byte type, int number, String text, String sender) {
        this.type = type;
        this.number = number;
        this.text = text;
        this.sender = sender;
    }

    public Message(byte type, byte[] data, String text, String sender) {
        this.type = type;
        this.data = data;
        this.text = text;
        this.sender = sender;
    }

    public byte getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    public byte[] getData() {
        return data;
    }

    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }
}
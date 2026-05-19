package com.example.quizapp;

public class Message {
    public static final int TYPE_USER   = 1;
    public static final int TYPE_BOT    = 2;
    public static final int TYPE_TYPING = 3; // indicateur "en train d'écrire"

    private String text;
    private int type;

    public Message(String text, int type) {
        this.text = text;
        this.type = type;
    }

    public String getText()         { return text; }
    public void   setText(String t) { this.text = t; }
    public int    getType()         { return type; }
    public void   setType(int t)    { this.type = t; }
}

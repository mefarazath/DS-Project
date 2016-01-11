package org.uom.cse.message;

public class MessageBuilder{

    private static final String zeros = "0000";
    private StringBuilder builder;

    public MessageBuilder(){
        builder = new StringBuilder();
    }

    public MessageBuilder append(String part) {
        builder.append(part).append(" ");
        return this;
    }

    public String buildMessage(){
        String msg = builder.toString().trim();

        // number of zeros to append
        int numZeros = 4 - (msg.length() + "").length();
        int totalMessageLength = msg.length() + 5;

        msg = zeros.substring(0,numZeros) + totalMessageLength + " " + msg;
        return msg;
    }
}

package cn.livestream.server.model.stream;

public class StartStreamResponse {
    private boolean success;
    private String message;
    private StreamInfo data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public StreamInfo getData() {
        return data;
    }

    public void setData(StreamInfo data) {
        this.data = data;
    }
}
package ru.meefik.linuxdeploy.model;

public class Mount {
    private String source;
    private String target;

    public Mount() {
    }

    public Mount(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}

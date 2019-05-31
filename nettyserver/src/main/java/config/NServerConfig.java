package config;

import httphandler.Action;
import httphandler.Filter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liudeyu on 2019/5/8.
 */
public class NServerConfig {

    private int port;


    public NServerConfig(int port) {
        this.port = port;
        if (port <= 1024) {
            throw new IllegalArgumentException("port should greater than 1024");
        }
    }


    public NServerConfig addAction(String path, Action action) {
        return this;
    }


    public NServerConfig addFilter(String path, Filter filter) {
        NServerSetting.setFilter(path,filter);
        return this;
    }


    public int getPort() {
        return port;
    }
}

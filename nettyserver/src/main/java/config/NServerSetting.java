package config;

import cn.hutool.core.util.StrUtil;
import httpconcept.Request;
import httpconcept.Response;
import httphandler.Action;
import httphandler.Filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by liudeyu on 2019/5/31.
 */
public class NServerSetting {

    public static Map<String, Filter> filterMap;
    public static Map<String, Action> actionMap;

    public static String ROOT_PATH = "/*";
    public static String ERROR_PATH = "/error";


    static {
        filterMap = new ConcurrentHashMap<>();
        actionMap = new ConcurrentHashMap<>();

        actionMap.put(ROOT_PATH, new Action() {
            @Override
            public void handleAction(Request request, Response response) {
                response.setContent("hello,this is netty simple server");
            }
        });
    }




    public static Action getSetTextAction(String text){
        return new Action() {
            @Override
            public void handleAction(Request request, Response response) {
                response.setContent(text);
            }
        };
    }





    public static Action getAction(String path){
        return actionMap.get(path);
    }
    public static Filter getRootFilter() {
        return filterMap.get(ROOT_PATH);
    }



    public static Action getRootAction(){
        return actionMap.get(ROOT_PATH);
    }




    public static Filter getFilter(String path) {
        return filterMap.get(path);
    }


    public static void setFilter(String path, Filter filter) {
        if (StrUtil.isEmpty(path)) {
            filterMap.put(ROOT_PATH, filter);
            return;
        }

        if (!path.startsWith(StrUtil.SLASH)) {
            path = StrUtil.SLASH + path;
        }
        filterMap.put(path,filter);
    }




}

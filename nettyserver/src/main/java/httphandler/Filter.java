package httphandler;

import httpconcept.Request;

/**
 * Created by liudeyu on 2019/5/8.
 */
public interface Filter {
    boolean filter(Request request);
}

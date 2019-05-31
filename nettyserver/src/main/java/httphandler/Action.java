package httphandler;

import httpconcept.Request;
import httpconcept.Response;

/**
 * Created by liudeyu on 2019/5/8.
 */
public interface Action {

    void handleAction(Request request, Response response);
}

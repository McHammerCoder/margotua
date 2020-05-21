package margotua.util;

import edu.columbia.cs.psl.phosphor.runtime.MultiTainter;
import edu.columbia.cs.psl.phosphor.runtime.Taint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StringListTainter {
    private int ctr;
    private String replacement;
    private Taint nextTaint() {
        return new Taint(Integer.toString(ctr++));
    }

    public StringListTainter(String replacement) {
        this.replacement = replacement;
        ctr = 1;
    }

    public StringListTainter() {
        this(null);
    }

    public List<String> taint(List<String> input) {
        MultiTainter.taintedObject(input, nextTaint());
        ArrayList temp = new ArrayList();
        for(Object item : input) {
            String s;
            if(replacement != null) {
                s = new String(replacement);
            } else {
                s = (String) item;
            }
            temp.add(new String(MultiTainter.taintedCharArray(s.toCharArray(), nextTaint())));
        }
        input.clear();
        input.addAll(temp);
        return input;
    }
}

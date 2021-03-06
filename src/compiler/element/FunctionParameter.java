package compiler.element;

public class FunctionParameter extends Element {

    private static String PREFIX = "$P";

    public FunctionParameter(int ctxVal, String name, Element.Type type) {
        super(Element.Context.FPARAM, ctxVal, name, type, null);
    }

    @Override
    public String toString() {
        return getRef() + " (" + getType() + " " + getName() + ")";
    }

    @Override
    public String getRef() {
        return PREFIX + getCtxVal();
    }

    @Override
    public Element getTinyElement(int localCount) {
        return new Stack(5 + getCtxVal(), getType());
    }
}

import cn.local.quickrecorder.DoublePress;
public class DoublePressTest {
    static void check(boolean x,String name) { if(!x) throw new AssertionError(name); }
    public static void main(String[] args) {
        DoublePress d = new DoublePress();
        check(!d.event(true,0,100),"first down"); check(!d.event(false,0,150),"first up");
        check(!d.event(true,0,300),"second down"); check(d.event(false,0,350),"double tap");
        check(!d.event(true,0,450),"bounce down"); check(!d.event(false,0,500),"bounce up");
        check(!d.event(true,0,550),"bounce2 down"); check(!d.event(false,0,600),"cooldown");
        d.reset(); d.event(true,0,2000); d.event(true,1,2100); check(!d.event(false,0,2400),"hold");
        d.event(true,0,2500); check(!d.event(false,0,2550),"after hold");
        d.reset(); d.event(true,0,3000); d.event(false,0,3050); d.event(true,0,3600); check(!d.event(false,0,3650),"slow taps");
        d.reset(); check(!d.event(false,0,4000),"orphan up");
        d.event(true,0,4100); d.event(false,0,4150); d.reset(); d.event(true,0,4250); check(!d.event(false,0,4300),"reset");
        System.out.println("PASS: double tap, repeats, long hold, cooldown, slow tap, orphan up, reset");
    }
}

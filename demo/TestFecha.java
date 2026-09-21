import com.example.Vista.SelectorFechaPanel;
import java.time.LocalDate;

public class TestFecha {
    public static void main(String[] args) {
        SelectorFechaPanel p = new SelectorFechaPanel();
        p.setFecha(LocalDate.of(2025, 12, 31));
        System.out.println("setFecha=" + p.getTextoActual());
        System.out.println("parseSet=" + p.obtenerFechaValidada());

        java.lang.reflect.Field f;
        try {
            f = SelectorFechaPanel.class.getDeclaredField("txtFecha");
            f.setAccessible(true);
            javax.swing.JTextField t = (javax.swing.JTextField) f.get(p);
            t.setText("2025-12-31");
            System.out.println("manual1=" + p.obtenerFechaValidada());

            t.setText("31/12/2025");
            System.out.println("manual2=" + p.obtenerFechaValidada());

            t.setText("2025/12/31");
            System.out.println("manual3=" + p.obtenerFechaValidada());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

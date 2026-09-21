import java.time.*;
import java.time.format.*;
public class DebugFecha {
    public static void main(String[] args) {
        String[] valores = {"2025-12-31", "31/12/2025", "31-12-2025", "2025/12/31", "12/31/2025"};
        DateTimeFormatter[] formatos = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd-MM-yyyy").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("MM/dd/yyyy").withResolverStyle(ResolverStyle.STRICT)
        };
        for (String v : valores) {
            System.out.println("VALOR=" + v);
            for (int i = 0; i < formatos.length; i++) {
                try {
                    LocalDate d = LocalDate.parse(v, formatos[i]);
                    System.out.println("  OK " + i + " => " + d);
                } catch (Exception e) {
                    System.out.println("  FAIL " + i + " => " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }
}

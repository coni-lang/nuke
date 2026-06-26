import generated.Person;

public class Main {
    public static void main(String[] args) {
        Person p = new Person();
        p.setName("Nico");
        System.out.println("Generated JAXB class via Nuke: " + p.getName());
    }
}

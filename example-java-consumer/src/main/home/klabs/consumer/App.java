package home.klabs.consumer;

import home.klabs.Main;

public class App {
    public static void main(String[] args) {
        // Call the greet() method from the my-app dependency
        String greeting = Main.greet("Consumer");
        System.out.println(greeting);
        System.out.println("Consumer app is running!");
    }
}

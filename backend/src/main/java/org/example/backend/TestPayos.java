package org.example.backend;

import vn.payos.PayOS;
import java.lang.reflect.Method;

public class TestPayos {
    public static void main(String[] args) {
        for (Method m : PayOS.class.getMethods()) {
            if (m.getName().toLowerCase().contains("webhook")) {
                Class<?> retType = m.getReturnType();
                for (Method m2 : retType.getMethods()) {
                    if (m2.getName().equals("verify")) {
                        System.out.println("Return type: " + m2.getReturnType().getName());
                    }
                }
            }
        }
    }
}

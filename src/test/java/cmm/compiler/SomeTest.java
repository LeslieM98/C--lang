package cmm.compiler;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;



public class SomeTest{
    @Test
    public void someTestMethod(){
        try{
            TimeUnit.SECONDS.sleep(20);
        } catch (Exception e){
            
        }
        Assertions.assertTrue(true);
    }
}
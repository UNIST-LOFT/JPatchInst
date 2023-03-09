public class Switch {
    public void switchTest(int a) {
        int b=a;
        if (b==1)
            System.out.println("b is 1!");
        else if (b==2)
            System.out.println("b is 2...");
        else
            System.out.println("b is not 1 or 2?");
            
        switch (a) {
            case 1:
                System.out.println("a is 1!");
                break;
            case 2:
                System.out.println("a is 2...");
                break;
            default:
                System.out.println("a is not 1 or 2?");
                break;
        }

        System.out.println("a is " + Integer.toString(a));
    }
}

package test;

public class Test {

	public static void main(String[] args) {
		
		String query = "我的一条狗";
		String term1 = "我的";
		String term2 = "我的老师";
		String term3 = "老师是我的";
	    
		System.out.println(new Float(term1.length())/new Float(term2.length()));
		
		System.out.println(new Float(term1.length())/new Float(term3.length()));
		
		System.out.println("老师是我的".indexOf("是"));
	}

}

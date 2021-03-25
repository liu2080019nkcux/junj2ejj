import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class PDASaveLog {
	
	public File actionLogFile;
	
	public File hpLogFile;
	
	public ArrayList <int[]> actionIntList;
	
	public ArrayList <int[]> hpIntList;	
	
	public PDASaveLog(){
		
		actionIntList = new ArrayList<int[]>();
		
		hpIntList = new ArrayList<int[]>();
		
		
		int i = 0;
		File hpLogFile = new File("./log/PDA/HP/HPPlayer" + i + ".csv");
		while (hpLogFile.exists()) {
			hpLogFile = new File("./log/PDA/HP/HPPlayer" + i + ".csv");
			i++;
		}
		
		int j = 0;				
		File actionLogFile = new File("./log/PDA/Action/ActionPlayer" + i + ".csv");
		while (actionLogFile.exists()) {
			actionLogFile = new File("./log/PDA/Action/ActionPlayer" + i + ".csv");
			j++;
		}
	}
	
	
	
	public void saveActionList(int[] actionList) {
		actionIntList.add(actionList);
	}
	
	public void saveHpList(int[] hpList) {
		hpIntList.add(hpList);		
	}
	
	public void saveActionLog()
	{
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(actionLogFile));
			
		} catch (IOException ex) {
			// 例外発生時処理
			ex.printStackTrace();
		}

	}
	
	public void saveHpLog()
	{
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(hpLogFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String convertArrayToCsv(int[] IncomingArray)
	{
	    StringBuilder sb=new StringBuilder();
	    for (int i=0;i<IncomingArray.length;i++)
	    {
	        sb=sb.append(String.valueOf(IncomingArray[i]));
	        if(i != IncomingArray.length-1)
	        {
	            sb.append(",");
	        }
	    }
	    return sb.toString();
	}//end of convertArrayToCsv method
}

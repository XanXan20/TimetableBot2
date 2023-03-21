package ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.word;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DocFileReader {
    String filePath = "/home/TimetableBot2/data/editedTimetable.docx";
    XWPFDocument doc;
    List<XWPFTable> tableList;

    public DocFileReader() throws IOException {
        doc = new XWPFDocument(new FileInputStream(filePath));

        Iterator<IBodyElement> docElementsIterator = doc.getBodyElementsIterator();
        while(docElementsIterator.hasNext()){
            IBodyElement docElement = docElementsIterator.next();
            if("TABLE".equalsIgnoreCase(docElement.getElementType().name())){
                tableList = docElement.getBody().getTables();
            }
        }
    }

    public Map<Integer, String> getChanges(Integer groupId){
        Map<Integer, String> timetableChanges = new HashMap<>();

        for (XWPFTable table : tableList){
            for(int i = 1; i < table.getRows().size(); i++){
                //разделение групп, если записано несколько в 1 ячейке (напр. 59, 60)
                String groupStr = table.getRow(i).getCell(0).getText();
                if(groupStr.contains(",")){
                    for (String item:
                         groupStr.split(",")) {
                        if(item.equals(groupId.toString())){
                            groupStr = item;
                        }
                    }
                }
                if(groupStr.equals(groupId.toString())){

                    String lesionRoom = table.getRow(i).getCell(4).getText();
                    if (lesionRoom.isEmpty())
                        lesionRoom = "- , - ";

                    String lesionNumber = table.getRow(i).getCell(1).getText();
                    String lesionName = table.getRow(i).getCell(3).getText() + ", " +
                            lesionRoom;

                    if(lesionNumber.contains(",")){
                        String[] someNumbers = lesionNumber.split(",");//это массив с уроками типа: 1,7-8
                        for (String item:
                             someNumbers) {
                             item = item.trim();
                             if(item.contains("-")) {
                                 String[] numbers = item.split("-");
                                 int startLesion = Integer.parseInt(numbers[0].trim());
                                 int endLesion = Integer.parseInt(numbers[1].trim());

                                 for (int j = startLesion; j <= endLesion; j++) {
                                     timetableChanges.put(j, lesionName);
                                 }
                             }
                             else
                                 timetableChanges.put(Integer.parseInt(item), lesionName);
                        }
                    }
                    else if(lesionNumber.contains("-")){
                        String[] numbers = lesionNumber.split("-");
                        int startLesion = Integer.parseInt(numbers[0].trim());
                        int endLesion = Integer.parseInt(numbers[1].trim());

                        for(int j = startLesion; j <= endLesion; j ++) {
                            timetableChanges.put(j, lesionName);
                        }
                    }
                    else if(lesionNumber.equals("")){
                        for(int j = 1; j <= 10; j++){
                            timetableChanges.put(j, lesionName);
                        }
                        System.out.println("ПП");
                    }else{
                        timetableChanges.put(Integer.parseInt(lesionNumber), lesionName);
                    }
                }
            }
        }

        return timetableChanges;
    }

    public int[] getData(){

        List<XWPFParagraph> paragraphList = doc.getParagraphs();

        String[] words = paragraphList.get(2).getText().split(" ");
        String[] dateS = words[6].split("\\.");

        int day = Integer.parseInt(dateS[0]);
        int month = Integer.parseInt(dateS[1]);
        int year = Integer.parseInt(dateS[2]);

        int[] dateInt = {day, month, year};

        return dateInt;
    }
}

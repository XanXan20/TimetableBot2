package ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XLSXFileReader {
    private final String filePath = "/home/TimetableBot2/data/actualTimetable.xlsx";
    private XSSFSheet sheet;
    private File file;
    private Map<Integer, Integer> groupIdToColumn;
    private ArrayList<Integer> groupIdList;

    public XLSXFileReader() throws IOException{
        this.file = new File(filePath);
        groupIdToColumn = new HashMap<>();
        groupIdList = new ArrayList<>();
        FileInputStream fis = new FileInputStream(filePath);
        XSSFWorkbook workbook = new XSSFWorkbook(fis);

        sheet = workbook.getSheet("Лист2");

        XSSFRow row = sheet.getRow(2);

        Iterator<Cell> cellIterator = row.cellIterator();

        while(cellIterator.hasNext()){
            Cell cell = cellIterator.next();

            int cellColumn = cell.getColumnIndex();
            int cellValue = (int)cell.getNumericCellValue();

            if (cellValue == 0.0)
                continue;

            groupIdList.add(cellValue);
            groupIdToColumn.put(cellValue, cellColumn);
        }
        workbook.close();
    }

    public void update(){

    }
    public Map<Integer, String> getGroupTimetable(int groupId, int day){

        Map<Integer, String> groupTimetable = new HashMap<>();

        int groupColumn = groupIdToColumn.get(groupId);
        int startRow = 3; //строка с которой начинается чтение расписания
        startRow += 10 * (day-1); //тут кароче отступ высчитывается для выбора дня недели

        for (int i = startRow; i < startRow+10; i++){
            XSSFRow row = sheet.getRow(i);
            try {
                XSSFCell cell = row.getCell(groupColumn);
                int numberCell = (int)row.getCell(groupColumn-1).getNumericCellValue();

                String lesionName = cell.getStringCellValue();

                if(!(lesionName.equals("") || lesionName.contains("Классный час"))){
                    boolean isInMerge = false;
                    int mergesCount = sheet.getNumMergedRegions();
                    for(int j = 0; j<mergesCount;j++){
                        CellRangeAddress cellRange = sheet.getMergedRegion(j);
                        if(cellRange.isInRange(cell)){
                            isInMerge=true;
                            int firstMergeRowId = cellRange.getFirstRow();
                            int lastMergedRowId = cellRange.getLastRow();

                            int lesionNumber = numberCell-1;
                            for(int k = firstMergeRowId; k <= lastMergedRowId; k++){
                                lesionNumber++;
                                groupTimetable.put(lesionNumber, lesionName);
                            }
                        }
                    }
                    if(!isInMerge){
                        groupTimetable.put(numberCell, lesionName);
                    }
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        return groupTimetable;
    }
    public ArrayList<Integer> getGroupIdList(){return groupIdList;}
    public File getFile(){return this.file;}
}

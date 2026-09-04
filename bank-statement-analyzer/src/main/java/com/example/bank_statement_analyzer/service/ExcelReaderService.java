package com.example.bank_statement_analyzer.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ExcelReaderService {

    public void readExcel(MultipartFile file) throws IOException {

        InputStream inputStream = file.getInputStream();

        Workbook workbook = WorkbookFactory.create(inputStream);

        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {

            for (Cell cell : row) {
                System.out.print(cell + " | ");
            }

            System.out.println();
        }

        workbook.close();
    }
}

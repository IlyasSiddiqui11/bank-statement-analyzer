package com.example.bank_statement_analyzer.controller;


import com.example.bank_statement_analyzer.service.ExcelReaderService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/excel")
public class ExcelReaderController {

    private final ExcelReaderService excelReader;

    public ExcelReaderController(ExcelReaderService excelReader) {
        this.excelReader = excelReader;
    }

    @PostMapping(
            value = "/read",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String readExcel(@RequestPart("file") MultipartFile file) throws IOException {

        excelReader.readExcel(file);

        return "Excel processed successfully";
    }
}
package com.ydl.crm.data.report.utils;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.metadata.Table;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.github.pagehelper.PageHelper;
import com.ydl.crm.data.report.constants.Constants;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author shisl
 * @package com.ydl.crm.data.report.utils
 * @class ExcelUtil
 * @email shisl@yidianling.com
 * @date 2022/4/14 下午3:11
 * @description
 */
public class ExcelUtil {
    /**
     * 阿里开源EXCEL 引入pom
     * <dependency>
     *  <groupId>com.alibaba</groupId>
     *  <artifactId>easyexcel</artifactId>
     *  <version>1.1.1</version>
     * </dependency>
     */

    /**
     * 针对较少的记录数(20W以内大概)可以调用该方法一次性查出然后写入到EXCEL的一个SHEET中
     * 注意： 一次性查询出来的记录数量不宜过大，不会内存溢出即可。
     *
     * @throws IOException
     */
    public ResultVO<Void> exportSysSystemExcel(SysSystemVO sysSystemVO, HttpServletResponse response) throws Exception {

        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            ExcelWriter writer = new ExcelWriter(out, ExcelTypeEnum.XLSX);

            // 设置EXCEL名称
            String fileName = new String(("SystemExcel").getBytes(), "UTF-8");

            // 设置SHEET名称
            Sheet sheet = new Sheet(1, 0);
            sheet.setSheetName("系统列表sheet1");

            // 设置标题
            Table table = new Table(1);
            List<List<String>> titles = new ArrayList<List<String>>();
            titles.add(Arrays.asList("系统名称"));
            titles.add(Arrays.asList("系统标识"));
            titles.add(Arrays.asList("描述"));
            titles.add(Arrays.asList("状态"));
            titles.add(Arrays.asList("创建人"));
            titles.add(Arrays.asList("创建时间"));
            table.setHead(titles);

            // 查数据写EXCEL
            List<List<String>> dataList = new ArrayList<>();
            List<SysSystemVO> sysSystemVOList = this.sysSystemReadMapper.selectSysSystemVOList(sysSystemVO);
            if (!CollectionUtils.isEmpty(sysSystemVOList)) {
                sysSystemVOList.forEach(eachSysSystemVO -> {
                    dataList.add(Arrays.asList(
                            eachSysSystemVO.getSystemName(),
                            eachSysSystemVO.getSystemKey(),
                            eachSysSystemVO.getDescription(),
                            eachSysSystemVO.getState().toString(),
                            eachSysSystemVO.getCreateUid(),
                            eachSysSystemVO.getCreateTime().toString()
                    ));
                });
            }
            writer.write0(dataList, sheet, table);

            // 下载EXCEL
            response.setHeader("Content-Disposition", "attachment;filename=" + new String((fileName).getBytes("gb2312"), "ISO-8859-1") + ".xls");
            response.setContentType("multipart/form-data");
            response.setCharacterEncoding("utf-8");
            writer.finish();
            out.flush();

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ResultVO.getSuccess("导出系统列表EXCEL成功");
    }


    /**
     * 针对105W以内的记录数可以调用该方法分多批次查出然后写入到EXCEL的一个SHEET中
     * 注意：
     * 每次查询出来的记录数量不宜过大，根据内存大小设置合理的每次查询记录数，不会内存溢出即可。
     * 数据量不能超过一个SHEET存储的最大数据量105W
     *
     * @throws IOException
     */
    public ResultVO<Void> exportSysSystemExcel(SysSystemVO sysSystemVO, HttpServletResponse response) throws Exception {

        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            ExcelWriter writer = new ExcelWriter(out, ExcelTypeEnum.XLSX);

            // 设置EXCEL名称
            String fileName = new String(("SystemExcel").getBytes(), "UTF-8");

            // 设置SHEET名称
            Sheet sheet = new Sheet(1, 0);
            sheet.setSheetName("系统列表sheet1");

            // 设置标题
            Table table = new Table(1);
            List<List<String>> titles = new ArrayList<List<String>>();
            titles.add(Arrays.asList("系统名称"));
            titles.add(Arrays.asList("系统标识"));
            table.setHead(titles);

            // 查询总数并 【封装相关变量 这块直接拷贝就行 不要改动】
            Integer totalRowCount = this.sysSystemReadMapper.selectCountSysSystemVOList(sysSystemVO);
            Integer pageSize = Constants.PER_WRITE_ROW_COUNT;
            Integer writeCount = totalRowCount % pageSize == 0 ? (totalRowCount / pageSize) : (totalRowCount / pageSize + 1);

            // 写数据 这个i的最大值直接拷贝就行了 不要改
            for (int i = 0; i < writeCount; i++) {
                List<List<String>> dataList = new ArrayList<>();

                // 此处查询并封装数据即可 currentPage, pageSize这个变量封装好的 不要改动
                PageHelper.startPage(i + 1, pageSize);
                List<SysSystemVO> sysSystemVOList = this.sysSystemReadMapper.selectSysSystemVOList(sysSystemVO);
                if (!CollectionUtils.isEmpty(sysSystemVOList)) {
                    sysSystemVOList.forEach(eachSysSystemVO -> {
                        dataList.add(Arrays.asList(
                                eachSysSystemVO.getSystemName(),
                                eachSysSystemVO.getSystemKey()
                        ));
                    });
                }
                writer.write0(dataList, sheet, table);
            }

            // 下载EXCEL
            response.setHeader("Content-Disposition", "attachment;filename=" + new String((fileName).getBytes("gb2312"), "ISO-8859-1") + ".xls");
            response.setContentType("multipart/form-data");
            response.setCharacterEncoding("utf-8");
            writer.finish();
            out.flush();

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ResultVO.getSuccess("导出系统列表EXCEL成功");
    }


    /**
     * 针对几百万的记录数可以调用该方法分多批次查出然后写入到EXCEL的多个SHEET中
     * 注意：
     * perSheetRowCount % pageSize要能整除  为了简洁，非整除这块不做处理
     * 每次查询出来的记录数量不宜过大，根据内存大小设置合理的每次查询记录数，不会内存溢出即可。
     *
     * @throws IOException
     */
    public ResultVO<Void> exportSysSystemExcel(SysSystemVO sysSystemVO, HttpServletResponse response) throws Exception {

        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            ExcelWriter writer = new ExcelWriter(out, ExcelTypeEnum.XLSX);

            // 设置EXCEL名称
            String fileName = new String(("SystemExcel").getBytes(), "UTF-8");

            // 设置SHEET名称
            String sheetName = "系统列表sheet";

            // 设置标题
            Table table = new Table(1);
            List<List<String>> titles = new ArrayList<List<String>>();
            titles.add(Arrays.asList("系统名称"));
            titles.add(Arrays.asList("系统标识"));
            titles.add(Arrays.asList("描述"));
            titles.add(Arrays.asList("状态"));
            titles.add(Arrays.asList("创建人"));
            titles.add(Arrays.asList("创建时间"));
            table.setHead(titles);

            // 查询总数并封装相关变量(这块直接拷贝就行了不要改)
            Integer totalRowCount = this.sysSystemReadMapper.selectCountSysSystemVOList(sysSystemVO);
            Integer perSheetRowCount = Constants.PER_SHEET_ROW_COUNT;
            Integer pageSize = Constants.PER_WRITE_ROW_COUNT;
            Integer sheetCount = totalRowCount % perSheetRowCount == 0 ? (totalRowCount / perSheetRowCount) : (totalRowCount / perSheetRowCount + 1);
            Integer previousSheetWriteCount = perSheetRowCount / pageSize;
            Integer lastSheetWriteCount = totalRowCount % perSheetRowCount == 0 ?
                    previousSheetWriteCount :
                    (totalRowCount % perSheetRowCount % pageSize == 0 ? totalRowCount % perSheetRowCount / pageSize : (totalRowCount % perSheetRowCount / pageSize + 1));


            for (int i = 0; i < sheetCount; i++) {

                // 创建SHEET
                Sheet sheet = new Sheet(i, 0);
                sheet.setSheetName(sheetName + i);

                // 写数据 这个j的最大值判断直接拷贝就行了，不要改动
                for (int j = 0; j < (i != sheetCount - 1 ? previousSheetWriteCount : lastSheetWriteCount); j++) {
                    List<List<String>> dataList = new ArrayList<>();

                    // 此处查询并封装数据即可 currentPage, pageSize这俩个变量封装好的 不要改动
                    PageHelper.startPage(j + 1 + previousSheetWriteCount * i, pageSize);
                    List<SysSystemVO> sysSystemVOList = sysSystemReadMapper.selectSysSystemVOList(sysSystemVO);
                    if (!CollectionUtils.isEmpty(sysSystemVOList)) {
                        sysSystemVOList.forEach(eachSysSystemVO -> {
                            dataList.add(Arrays.asList(
                                    eachSysSystemVO.getSystemName(),
                                    eachSysSystemVO.getSystemKey(),
                                    eachSysSystemVO.getDescription(),
                                    eachSysSystemVO.getState().toString(),
                                    eachSysSystemVO.getCreateUid(),
                                    eachSysSystemVO.getCreateTime().toString()
                            ));
                        });
                    }
                    writer.write0(dataList, sheet, table);
                }
            }

            // 下载EXCEL
            response.setHeader("Content-Disposition", "attachment;filename=" + new String((fileName).getBytes("gb2312"), "ISO-8859-1") + ".xls");
            response.setContentType("multipart/form-data");
            response.setCharacterEncoding("utf-8");
            writer.finish();
            out.flush();

        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ResultVO.getSuccess("导出系统列表EXCEL成功");
    }


}



#### 先进先出法，计算出库成本

```visual basic
Option Explicit
Public Sub CBJS()
    Dim WS1 As Worksheet, LASTROW1 & , WS2 As Worksheet, LASTROW2 & , KCRR, TZRR, JGRR(), I & , J & , K & , L & , TEMP#, ID
    Set WS1 = Sheets("库存表")
    Set WS2 = Sheets("入库出库台账")
    LASTROW1 = WS1.Range("C" & WS1.Rows.Count).End(xlUp).Row
    LASTROW2 = WS2.Range("F" & WS2.Rows.Count).End(xlUp).Row
    WS2.Range("J4:L" & LASTROW2).ClearContents '清空成本，成本金额，备注内容
    KCRR = WS1.Range("A5:I" & LASTROW1).Value '库存数组
    TZRR = WS2.Range("A4:I" & LASTROW2).Value '台帐数组
    ReDim JGRR(1 To UBound(TZRR), 1 To 3) '结果数组----成本，成本金额，备注
    For I = 1 To UBound(TZRR) 'I和结果数组同步
        If TZRR(I, 6) Like "*出库*" Then
            ID = TZRR(I, 5) '需出库的品名
            TEMP = TZRR(I, 7) '需出库的数量
            For J = 1 To UBound(KCRR) '先去库存找
                If KCRR(J, 3) = ID And KCRR(J, 7) > 0 Then
                    If TEMP <= KCRR(J, 7) Then
                        JGRR(I, 2) = JGRR(I, 2) + TEMP * KCRR(J, 8)
                        KCRR(J, 7) = KCRR(J, 7) - TEMP
                        TEMP = 0
                        Exit For
                    ElseIf TEMP > KCRR(J, 7) Then
                        JGRR(I, 2) = JGRR(I, 2) + KCRR(J, 7) * KCRR(J, 8)
                        TEMP = TEMP - KCRR(J, 7)
                        KCRR(J, 7) = 0
                    End If
                End If
            Next J
            If TEMP > 0 Then   '库存找完，如果已经完成，不再找，否则，继续在台帐里面找
            For L = 1 To I - 1 '出入库台帐只在记录上面找
                If TZRR(L, 6) Like "*入库*" And TZRR(L, 7) > 0 And TZRR(L, 5) = ID Then
                    If TEMP <= TZRR(L, 7) Then
                        JGRR(I, 2) = JGRR(I, 2) + TEMP * TZRR(L, 8)
                        TZRR(L, 7) = TZRR(L, 7) - TEMP
                        TEMP = 0
                        Exit For
                    ElseIf TEMP > TZRR(L, 7) Then
                        JGRR(I, 2) = JGRR(I, 2) + TZRR(L, 7) * TZRR(L, 8)
                        TEMP = TEMP - TZRR(L, 7)
                        TZRR(L, 7) = 0
                    End If
                End If
            Next L
        End If'计算成本金额完成，开始计算成本
        If TEMP > 0 Then JGRR(I, 3) = "库存不足，缺" & TEMP
        If JGRR(I, 2) > 0 Then JGRR(I, 1) = JGRR(I, 2) / (TZRR(I, 7) - TEMP)
    End If
Next I
WS2.Range("J4").Resize(UBound(JGRR), 3).Value = JGRR
End Sub
```



#### 复制表格

```visual basic
Sub MergeInput()
    Dim source_file As Workbook
    Dim destination_file As Workbook
    Dim source_sheet As Worksheet
    Dim destination_sheet As Worksheet
    ' 选择要合并数据的源工作簿和目标工作簿
    Set source_file = Workbooks.Open(ThisWorkbook.Path & "/入库表.xlsx")
    Set destination_file = Workbooks.Open(ThisWorkbook.Path & "/存货计价测试-先进先出法.xls")
      
    ' 选择要合并数据的源工作表和目标工作表
    Set source_sheet = source_file.Worksheets("Sheet1")
    Set destination_sheet = destination_file.Worksheets("入库表")

    ' 复制数据
    source_sheet.UsedRange.Copy Destination:=destination_sheet.Range("A1")

    ' 保存合并后的文件
    source_file.Save
    ' 可选：清除剪贴板以避免内存泄漏
    Application.CutCopyMode = False

    MsgBox "入库表已复制到目标表格中！", vbInformation


End Sub

```


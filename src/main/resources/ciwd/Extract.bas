Attribute VB_Name = "Module1"
Option Explicit

Public Sub Extract()

    On Error Resume Next ' Enable error handling

    Dim xlApp
    Set xlApp = Application
    xlApp.EnableEvents = True ' Enable events
  
    ' Set the source worksheet (adjust sheet name as needed)
    Dim wb As Workbook
    Dim ws As Worksheet
    Set wb = xlApp.Workbooks.Item(xlApp.Workbooks.Count())
    Set ws = wb.Sheets("Data")

    ' Check if the sheet exists
    If ws Is Nothing Then
        MsgBox "Sheet 'Sheet1' not found!", vbExclamation
        Exit Sub
    End If

    ' Find the last row with data in column A
    Dim lastRow As Long
    lastRow = ws.Cells(ws.Rows.Count, "A").End(xlUp).Row

    ' Check if there is data in column A
    If lastRow < 1 Then
        MsgBox "No data found in column A!", vbExclamation
        Exit Sub
    End If

    ' Define the source ranges for columns A and AC
    Dim sourceRangeA As Range
    Set sourceRangeA = ws.Range("A1:A" & lastRow)

    Dim sourceRangeAC As Range
    Set sourceRangeAC = ws.Range("AC1:AC" & lastRow)

    ' Get the directory path of the input XLSX file
    Dim xlsxFilePath As String
    xlsxFilePath = wb.FullName

    ' Create a new text file in the same directory as the input XLSX file
    Dim filePath As String
    filePath = xlsxFilePath & ".txt"
    Open filePath For Output As #1

    ' Write the headers to the text file
    Print #1, "Column A" & vbTab & "Column AC"

    ' Write data from source ranges to the text file
    Dim i As Long
    For i = 1 To lastRow
        Print #1, sourceRangeA.Cells(i, 1).Value & vbTab & sourceRangeAC.Cells(i, 1).Value
    Next i

    ' Close the text file
    Close #1

    MsgBox "Columns A and AC extracted and saved to a text file successfully!", vbInformation

End Sub

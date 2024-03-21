Attribute VB_Name = "Module11"
Option Explicit


Sub ExcelMacroExample()

  

  Dim xlApp
  Dim xlBook
  
  Set xlApp = Application
  xlApp.EnableEvents = True ' Enable events
  
  Set xlBook = xlApp.Workbooks.Open("HLA-B_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close
  
  Set xlBook = xlApp.Workbooks.Open("HLA-C_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close
  
  Set xlBook = xlApp.Workbooks.Open("HLA-DPB1_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close
  
  Set xlBook = xlApp.Workbooks.Open("HLA-DQB1_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close
  
  Set xlBook = xlApp.Workbooks.Open("HLA-DRB1_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close
  
  Set xlBook = xlApp.Workbooks.Open("HLA-DRB3_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close
  
  Set xlBook = xlApp.Workbooks.Open("HLA-DRB4_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close
  
  Set xlBook = xlApp.Workbooks.Open("HLA-DRB5_PrimaryData-IHWS-20200320.xlsx", 0, True)
  xlApp.Run "Extract"
  xlBook.Close

  Set xlBook = Nothing
  Set xlApp = Nothing

End Sub


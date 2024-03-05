 grep -v "N/A" *.txt | grep -v "Total" | sed "s/:/@/" | sed "s/.*@//" > ciwd300.txt

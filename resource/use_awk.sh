awk -F" " '{for (i=1;i<=NF;++i){if ($i ~ /^[0-9]/){continue} printf("%s ", $i) } print ""}  ' queries_expandedterm5.txt | awk -F" " '{for (i = 2; i <= NF; i++) {printf("%s ", $i)} print ""}' > a
awk -F" " '{for (i=1;i<=NF;++i){if ($i ~ /^[0-9]/){continue} printf("%s ", $i) } print ""}  ' queries_expandedterm50.txt | awk -F" " '{for (i = 2; i <= NF; i++) {printf("%s ", $i)} print ""}' > b
awk -F" " '{for (i=1;i<=NF;++i){if ($i ~ /^[0-9]/){continue} printf("%s ", $i) } print ""}  ' queries_expandeddoc40.txt | awk -F" " '{for (i = 2; i <= NF; i++) {printf("%s ", $i)} print ""}' > c

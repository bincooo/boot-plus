OLD=Boot
NEW=Boot

# 1. 替换文件内容（含字符串、package、import）
grep -rIl --exclude-dir={.git,.idea,target,build,node_modules} "$OLD" . \
  | xargs sed -i "s/$OLD/$NEW/g"
# macOS 的 sed 需写成：sed -i '' "s/$OLD/$NEW/g"

# 2. 重命名文件和目录（-depth 保证先改子级再改父级，包目录也会被处理）
find . -depth -name "*$OLD*" -not -path "./.git/*" -not -path "./.idea/*" | while read -r p; do
  d=$(dirname "$p"); b=$(basename "$p")
  git mv "$p" "$d/${b//$OLD/$NEW}"    # 非 git 项目改用 mv
done

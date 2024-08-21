# Action

U盘根目录有".skipUDT"文件将不会进行任何操作

# index.db(仅存储客观数据)

## Tables

- `udisks` - stores udisk information

udisk_id | name | totalSize | currentSize

- `dirs` - stores all dir tree information

dirname | dir_id | parent_dir_id | is_dir | create_time

- `<udisk_id>` - stores udisk all files information

filename | file_id | parent_dir_id | size | create_time
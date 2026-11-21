# DEC-01 — Hướng khởi tạo Pawn

## Mục đích

Khởi tạo currentDirection theo TD v0.1, mục 5.5.
Pawn tìm đường tới hàng phong cấp row = 0, không tìm tới Knight.

## Quy tắc

- Dùng BFS tám hướng, mỗi bước cost 1.
- Cho phép đi chéo, không kiểm tra thêm hai ô cạnh góc.
- Đá và các Enemy khác là vật cản.
- Bỏ qua Knight khi tính đường khởi tạo.
- Đích là bất kỳ ô không bị chặn trên hàng row = 0.
- Chỉ xét bước đầu thuộc đường ngắn nhất.
- Nếu có nhiều lựa chọn, ưu tiên bước SOUTH thẳng xuống;
  sau đó chọn column nhỏ nhất, rồi row nhỏ nhất.
- Không có đường ban đầu: dùng SOUTH.
- NORTH tăng row; SOUTH giảm row.
- Tính hướng không di chuyển quân, không tăng lượt.

## Tích hợp

- BattleFactory đặt đầy đủ đá và quân trước khi tính hướng.
- Mọi Pawn được tính từ cùng một snapshot ban đầu.
- Hướng được lưu trong PieceState và BattleSnapshot.PieceView.
- withHp giữ nguyên hướng.
- Quân không phải Pawn có currentDirection = null.
- Constructor cũ mặc định Pawn là SOUTH;
  Factory gán lại hướng bằng resolver trước khi trả Battle.

## Map01

- pawn-1 tại c7: SOUTH.
- pawn-2 tại d7: SOUTH.

Cả hai đều có bước thẳng SOUTH thuộc đường ngắn nhất
tới hàng phong cấp.

## Sửa sai so với bản trước

Bản trước tìm đường tới Knight và chỉ phá hòa theo column/row.
Bản này sửa đích thành row = 0 và ưu tiên bước thẳng SOUTH
trong các đường ngắn nhất, theo TD mục 5.5.

## Kiểm tra

- Không hướng về Knight khi Knight nằm phía sau Pawn.
- Ưu tiên SOUTH khi các đường ngắn bằng nhau.
- Tránh đá và Enemy khác.
- Bỏ qua Knight trong phép tính đường ban đầu.
- Không có đường thì SOUTH.
- Không thêm corner blocking.
- Ưu tiên đường ngắn trước ưu tiên hướng thẳng.
- Truy vấn không thay đổi Battle.
- Hai Pawn Map01 được khởi tạo đúng hướng.

## Giới hạn

Chỉ xử lý hướng khởi tạo.
Tính lại đường, chặn Knight ở bước đầu, fallback và hành động
trong activation thuộc phần logic Pawn riêng.
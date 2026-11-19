# DEC-01 bổ sung — Hướng khởi tạo Pawn

Trạng thái: Đã chọn và triển khai trên branch T02.03;
chờ nhóm review qua PR.

## Phạm vi

Tính currentDirection khi khởi tạo Battle.
Không triển khai hành động hoặc tính lại hướng trong lượt địch.

## Quyết định

- Dùng BFS tám hướng, mỗi bước có cost 1.
- Cho phép đi chéo, không kiểm tra thêm hai ô cạnh góc.
- Đá và các quân khác chặn đường.
- Ô Pawn đang xét là điểm xuất phát; ô Knight là đích BFS.
  Tìm tới Knight không đồng nghĩa được Move vào ô Knight.
- Nếu nhiều đường ngắn bằng nhau, ưu tiên ô bước đầu theo
  column tăng dần, rồi row tăng dần.
- Duyệt các ô lân cận theo column rồi row tăng dần
  để kết quả có tính xác định.
- Hướng khởi tạo là hướng từ Pawn tới bước đầu được chọn.
- Không có đường: dùng SOUTH.
- NORTH tăng row; SOUTH giảm row;
  EAST tăng column; WEST giảm column.
- Tính hướng không di chuyển quân và không tăng lượt.

## Tích hợp

- Factory đặt đầy đủ đá và quân trước khi tính hướng.
- Tính hướng của mọi Pawn từ cùng một snapshot ban đầu.
- Lưu hướng trong PieceState và xuất qua BattleSnapshot.PieceView.
- withHp giữ nguyên hướng.
- Quân không phải Pawn có currentDirection = null.
- Giữ constructor cũ để tương thích:
  Pawn mặc định SOUTH; các quân khác mặc định null.
- Factory phải gán lại hướng bằng BFS trước khi trả Battle.

## Kết quả Map01

Theo quy tắc trên:
- pawn-1 tại c7: SOUTH_WEST, bước đầu b6.
- pawn-2 tại d7: SOUTH_WEST, bước đầu c6.

## Kiểm tra

- Resolver: đường chéo, phân xử hòa, đá chặn, quân chặn,
  không có đường, corner rule và đầu vào không hợp lệ.
- State/snapshot: giữ hướng khi đổi HP, snapshot cũ không đổi.
- Factory: hai Pawn có hướng đúng; vị trí và lượt không đổi.
- Chạy test cả module logic và core.

## Phần thuộc gameplay sau này

Quy tắc tính lại hướng, fallback Move1 và Attack/MoveAttack
trong activation được triển khai riêng theo DEC-01 của TD.
Không dùng nguyên hàm initialDirection cho gameplay
vì quy tắc bước đầu tới ô Knight có khác biệt.
# DEC-12 — Fail-stop khi config lỗi hoặc AI sinh request invalid

- **Trạng thái:** Approved — xác nhận của người yêu cầu ngày 27/11/2026; reviewer/nhóm rà soát khi tích hợp.
- **Người xác nhận:** Người yêu cầu trong phiên làm việc ngày 27/11/2026.
- **Căn cứ:** RogueKnight TD v0.1, mục 8.2 DEC-12; CODEBASE_OVERVIEW.md, mục 5.
- **Task ảnh hưởng:** CODE-01/03/06/10; config loading, EnemyTurnRunner, controller/game flow và runtime error presentation.
- **Phạm vi:** Cách xử lý cấu hình không hợp lệ trước trận và AI tạo request không hợp lệ trong trận.

## Phương án được chọn: fail-stop

Lỗi cấu hình và request invalid do AI không được âm thầm bỏ qua hoặc giả lập như một action hợp lệ.

### 1. Config lỗi trước khi bắt đầu Battle

- Validate dữ liệu cấu hình trước khi tạo/bắt đầu Battle.
- Nếu JSON sai cú pháp, thiếu dữ liệu bắt buộc hoặc vi phạm schema/invariant, dừng khởi tạo Battle.
- Phát sinh lỗi có nguyên nhân rõ ràng (file/key/field và lý do nếu xác định được), ghi log hoặc trả setup error cho lớp khởi chạy.
- Không tạo một trận với dữ liệu mặc định ngầm hoặc dữ liệu thiếu một phần.

### 2. AI sinh ActionRequest invalid trong Battle

- Xem đây là bug tích hợp/logic, không phải lựa chọn Skip của AI.
- Không chuyển request invalid thành Skip, không retry vô hạn và không gọi ActionResolver với request đó.
- Phát sinh exception/runtime error rõ ràng; log tối thiểu enemy ID, global turn, phase, request/action kind, error code và vị trí xử lý.
- Dừng Enemy queue ngay lập tức; không chạy activation tiếp theo, không nhận thêm input gameplay và không tiếp tục trận.
- Các action hợp lệ đã hoàn tất trước khi phát hiện lỗi được giữ nguyên; **không rollback toàn bộ Player/Enemy phase**. Request invalid hiện tại không được apply.
- Trận bị đánh dấu/kết thúc ở trạng thái runtime failure riêng với Victory/Defeat. Không tính lỗi runtime là người chơi thắng hoặc thua. UI đưa người chơi tới thông báo lỗi và cho phép quay về menu/restart; không tiếp tục từ state dở dang.
- Lỗi phải được truyền tới ranh giới ứng dụng để hiển thị/log; không bắt exception rồi âm thầm tiếp tục queue.

## Phương án không chọn

- Âm thầm Skip một AI request invalid như một quyết định AI bình thường.
- Retry hoặc chọn một action thay thế không có căn cứ.
- Rollback toàn bộ lượt/phase đã có các action hợp lệ hoàn tất.
- Tiếp tục trận sau khi AI sinh request invalid.
- Ghi nhận runtime failure thành Victory hoặc Defeat.

## Tiêu chí nghiệm thu

- [ ] Config sai bị từ chối trước khi Battle bắt đầu; lỗi có reason đủ để xác định dữ liệu hỏng.
- [ ] AI request invalid không tới ActionResolver và không tạo mutation event cho request đó.
- [ ] Exception/error có context gồm enemy ID, turn, phase và error code/request.
- [ ] Enemy queue dừng tại lỗi; không có activation hoặc action sau lỗi.
- [ ] Input gameplay bị khóa và không thể tiếp tục trận từ state lỗi.
- [ ] Các action đã hoàn tất trước lỗi được giữ; không rollback; request invalid không làm thay đổi state.
- [ ] Runtime failure được phân biệt với Victory/Defeat và có đường thoát về menu/restart.
- [ ] Test bao phủ invalid request ở đầu queue và sau ít nhất một action hợp lệ đã hoàn tất trong cùng Enemy phase.
- [ ] Test xác nhận không có action/activation nào chạy sau lỗi.

## Ghi chú triển khai

- CODEBASE_OVERVIEW ghi nhận EnemyTurnRunner hiện ném IllegalStateException khi AI chọn request invalid, không âm thầm Skip. Đây là hành vi phù hợp với nguyên tắc fail-fast ở tầng logic nhưng chưa tự chứng minh toàn bộ chính sách fail-stop ở tầng ứng dụng đã hoàn chỉnh.
- Cần xác định một error boundary ở BattleController/game flow/Main để dừng playback/input/queue và hiển thị runtime failure. Không để exception làm sập desktop process mà không có thông tin hữu ích; cũng không nuốt exception để trận tiếp tục.
- Có thể cần bổ sung trạng thái runtime failure ở application/presentation layer. Không tự thêm Victory/Defeat enum hoặc thay đổi domain result nếu chưa đánh giá contract hiện tại.
- Nếu lỗi phát sinh sau một số action trong phase, giữ nguyên các action đã commit trước đó; không cố giả vờ trận vẫn có thể chơi tiếp.

## Theo dõi

- TD v0.1 đang được giữ nguyên theo yêu cầu; tài liệu này là decision record tạm trong docs/decisions/.
- Khi TD được mở lại, cập nhật DEC-12, error flow và runtime presentation tương ứng.
- Commit triển khai và reviewer: ghi tại PR/task triển khai sau này.

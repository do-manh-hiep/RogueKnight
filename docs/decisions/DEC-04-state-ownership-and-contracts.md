# DEC-04 — State ownership và contract request/result/options

- **Trạng thái:** Approved — xác nhận của người yêu cầu ngày 27/11/2026; reviewer/nhóm rà soát khi tích hợp.
- **Người xác nhận:** Người yêu cầu trong phiên làm việc ngày 27/11/2026.
- **Căn cứ:** RogueKnight TD v0.1, mục 4 (state/contract), mục 8.2 DEC-04; CODEBASE_OVERVIEW.md, mục 2–4.
- **Task ảnh hưởng:** CODE-02/03/04/09 và các integration task sử dụng battle state/controller.
- **Phạm vi:** Quyền sở hữu mutable gameplay state và ranh giới request/result/options giữa logic, AI và UI. Không thay đổi luật gameplay.

## Câu hỏi cần quyết định

Ai được phép đọc và thay đổi battle state? Các caller có được tự định nghĩa API hoặc giữ mutable state để xử lý hành động hay không?

## Phương án được chọn

1. **BattleState là aggregate có thẩm quyền duy nhất** của một trận, sở hữu Board, PieceState, Inventory, turn, phase và result. Không tạo bản mutable gameplay state thứ hai ở UI hoặc AI.
2. **BattleController là entry point điều phối action/lifecycle.** Caller gửi ActionRequest qua contract đã thống nhất; controller kiểm tra phase/result, gọi validation, điều phối resolver, turn và enemy phase.
3. **Rules chỉ query/validate.** KnightRules và EnemyRules đọc state/view cần thiết, trả kết quả validation hoặc legal options; không tự mutate state.
4. **ActionResolver là nơi áp dụng gameplay mutation** cho một plan đã được validation chấp thuận. Plan là chi tiết nội bộ, được tạo và apply trong cùng luồng xử lý; caller bên ngoài không tự tạo plan hợp lệ hoặc gọi resolver để bỏ qua controller.
5. **AI chỉ đọc state dạng read-only** (StateView hoặc contract tương đương), trả quyết định/request. AI không giữ BattleState mutable và không trực tiếp áp dụng action.
6. **UI/core chỉ gửi request và tiêu thụ kết quả** qua snapshot bất biến, event có thứ tự và các lựa chọn được cung cấp bởi logic. UI không sửa gameplay state, không tự xác nhận legal action và không suy diễn state tương lai.
7. Dùng chung một bộ contract request/result/options/error code. Không để từng caller tự tạo biến thể API riêng. Request invalid trả lỗi xác định, không gọi Resolver, không thay đổi state và không phát mutation events.

## Phương án không chọn

- Cho UI hoặc AI giữ/thay đổi BattleState mutable.
- Cho Rules vừa validate vừa tự áp dụng mutation.
- Cho nhiều caller tự định nghĩa request/result/options hoặc tự tạo ActionPlan.
- Để UI hoặc AI gọi trực tiếp các mutation method của BattleState/Board/PieceState.

## Hệ quả và ràng buộc triển khai

- Đây là quyết định về boundary/ownership, không phải yêu cầu đổi tên class hoặc viết lại toàn bộ domain.
- Các mutation method hiện hữu của BattleState không được làm public API cho caller bên ngoài. Phải rà soát toàn bộ call sites trước khi thu hẹp visibility; không đổi visibility một cách cơ học nếu Factory/Resolver/EnemyTurnRunner còn cần quyền nội bộ qua package khác.
- Những luồng nội bộ như EnemyPhase/EnemyTurnRunner phải nhận quyền tối thiểu cần thiết thay vì được cấp toàn bộ mutable aggregate. Chọn cơ chế cụ thể (command/capability/internal port hoặc tổ chức package) trong task triển khai, miễn giữ nguyên ownership và không mở mutation cho UI/AI.
- BattleSnapshot/StateView là read model; snapshot phải detached/immutable theo contract. BattlePresentationModel chỉ là projection hiển thị, không trở thành gameplay authority.
- Không thay đổi thứ tự event, luật lượt, scheduler, AI policy hoặc kết quả combat chỉ để phục vụ refactor boundary.

## Tiêu chí nghiệm thu

- [ ] logic vẫn pure Java; core không truy cập mutable BattleState hoặc mutation API.
- [ ] AI/policy chỉ đọc read-only view và trả decision/request; không mutate state.
- [ ] Controller là cổng điều phối action/lifecycle dùng bởi game flow.
- [ ] Rules query/validate không mutate state.
- [ ] Resolver chỉ áp dụng plan nội bộ đã validate; caller bên ngoài không thể bypass validation/controller.
- [ ] Invalid request giữ nguyên toàn bộ gameplay state và không phát mutation events.
- [ ] Snapshot/view không cho caller sửa ngược state; snapshot cũ không đổi khi trận tiếp tục.
- [ ] Legal options, error codes và request/result được dùng nhất quán giữa UI và AI.
- [ ] Test logic hiện có tiếp tục pass; bổ sung test boundary/contract cho các quyền truy cập và luồng invalid.

## Không nằm trong quyết định này

- Thay đổi gameplay rules, AI ranking/policy, balance hoặc data schema.
- Chọn ngay một kỹ thuật Java cụ thể để enforce package/module boundary trước khi audit call sites.
- Thiết kế lại presentation playback hoặc animation.

## Theo dõi

- TD v0.1 đang được giữ nguyên theo yêu cầu; tài liệu này là decision record tạm trong docs/decisions/.
- Khi TD được mở lại, cập nhật DEC-04 và các contract/diagram liên quan trong TD; không để hai tài liệu lệch nhau.
- Commit triển khai và reviewer: ghi tại PR/task triển khai sau này.

# IMPLEMENTATION_PLAN.md

This document outlines the implementation plan for the point management system, following TDD principles.

## Project Goal

Implement a user point management system with the following features:
1. Point inquiry
2. Point transaction history inquiry
3. Point charging
4. Point usage

## Implementation Strategy

### Phase 1: Point Inquiry Feature
**Goal:** Implement `GET /point/{id}` endpoint to retrieve user points

**Test Cases:**
- [ ] Should return user point with zero balance for new user
- [ ] Should return existing user point with current balance
- [ ] Should return correct updateMillis timestamp

**Implementation Steps:**
1. Create `PointService` class
2. Implement `getUserPoint(long userId)` method
3. Wire service to `PointController.point()` method
4. Verify all tests pass

**Business Rules:**
- New users start with 0 points
- Must return current point balance and last update time

---

### Phase 2: Point Charging Feature
**Goal:** Implement `PATCH /point/{id}/charge` endpoint to add points to user account

**Test Cases:**
- [ ] Should successfully charge valid positive amount
- [ ] Should update user point balance correctly
- [ ] Should record CHARGE transaction in history
- [ ] Should reject negative or zero amounts
- [ ] Should reject amounts exceeding maximum allowed (if applicable)
- [ ] Should handle concurrent charge requests safely

**Implementation Steps:**
1. Implement `chargePoint(long userId, long amount)` in `PointService`
2. Add input validation (amount > 0)
3. Update user point balance using `UserPointTable`
4. Record transaction in `PointHistoryTable` with type CHARGE
5. Wire service to `PointController.charge()` method
6. Verify all tests pass

**Business Rules:**
- Amount must be positive (> 0)
- Must atomically update balance and create history record
- Transaction type must be CHARGE
- Maximum charge amount: TBD (consider adding limit)

---

### Phase 3: Point Usage Feature
**Goal:** Implement `PATCH /point/{id}/use` endpoint to deduct points from user account

**Test Cases:**
- [ ] Should successfully use points when balance is sufficient
- [ ] Should update user point balance correctly
- [ ] Should record USE transaction in history
- [ ] Should reject when balance is insufficient
- [ ] Should reject negative or zero amounts
- [ ] Should reject when resulting balance would be negative
- [ ] Should handle concurrent use requests safely

**Implementation Steps:**
1. Implement `usePoint(long userId, long amount)` in `PointService`
2. Add input validation (amount > 0)
3. Add balance validation (balance >= amount)
4. Update user point balance using `UserPointTable`
5. Record transaction in `PointHistoryTable` with type USE
6. Wire service to `PointController.use()` method
7. Verify all tests pass

**Business Rules:**
- Amount must be positive (> 0)
- User must have sufficient balance (current points >= amount)
- Balance cannot go negative
- Must atomically update balance and create history record
- Transaction type must be USE

---

### Phase 4: Point History Inquiry Feature
**Goal:** Implement `GET /point/{id}/histories` endpoint to retrieve point transaction history

**Test Cases:**
- [ ] Should return empty list for user with no transactions
- [ ] Should return all transactions for user in chronological order
- [ ] Should include both CHARGE and USE transactions
- [ ] Should return correct transaction details (amount, type, timestamp)
- [ ] Should only return transactions for specified user

**Implementation Steps:**
1. Implement `getPointHistory(long userId)` in `PointService`
2. Fetch history from `PointHistoryTable`
3. Wire service to `PointController.history()` method
4. Verify all tests pass

**Business Rules:**
- Return all transactions for the specified user
- Transactions should be ordered (consider ordering by timestamp or id)
- Each history entry must include: id, userId, amount, type, updateMillis

---

### Phase 5: Concurrency & Thread Safety
**Goal:** Ensure thread-safe operations for concurrent point transactions

**Test Cases:**
- [ ] Should handle concurrent charges to same user correctly
- [ ] Should handle concurrent uses from same user correctly
- [ ] Should handle mixed concurrent charge/use operations correctly
- [ ] Should prevent race conditions in balance updates
- [ ] Final balance should be consistent after concurrent operations

**Implementation Steps:**
1. Identify critical sections in `PointService`
2. Implement synchronization mechanism (consider `synchronized` or `ReentrantLock`)
3. Add integration tests for concurrent scenarios
4. Verify thread safety through stress testing

**Concurrency Strategy:**
- Use user-level locking to prevent race conditions
- Consider using `ConcurrentHashMap` with user ID as key for locks
- Ensure atomic read-check-update operations

---

## Exception Handling Strategy

### Custom Exceptions to Implement:
- `InvalidAmountException` - For negative or zero amounts
- `InsufficientPointsException` - For insufficient balance during usage
- `PointNotFoundException` - If needed for missing user points (optional)

### Error Response Mapping:
- Invalid amount → 400 Bad Request
- Insufficient points → 400 Bad Request
- General errors → 500 Internal Server Error (existing)

### Implementation:
1. Create custom exception classes
2. Add specific `@ExceptionHandler` methods in `ApiControllerAdvice`
3. Return appropriate HTTP status codes and error messages

---

## Testing Strategy

### Unit Tests:
- Test `PointService` methods in isolation
- Mock `UserPointTable` and `PointHistoryTable` dependencies
- Focus on business logic and validation

### Integration Tests:
- Test full flow from controller to database tables
- Use actual `UserPointTable` and `PointHistoryTable` instances
- Verify end-to-end behavior

### Concurrency Tests:
- Use `ExecutorService` to simulate concurrent requests
- Verify final state consistency
- Test with multiple threads accessing same user

---

## Development Checklist

### Setup:
- [x] Repository structure analyzed
- [x] CLAUDE.md created
- [x] IMPLEMENTATION_PLAN.md created
- [ ] Initial test structure created

### Implementation Order:
1. [ ] Phase 1: Point Inquiry
2. [ ] Phase 2: Point Charging
3. [ ] Phase 3: Point Usage
4. [ ] Phase 4: Point History Inquiry
5. [ ] Phase 5: Concurrency & Thread Safety
6. [ ] Exception Handling
7. [ ] Final integration testing
8. [ ] Code coverage verification

---

## Notes & Considerations

### Design Decisions:
- **Service Layer:** Implement `PointService` to separate business logic from controller
- **Transaction Management:** Since using in-memory tables without real transactions, implement manual rollback logic if needed
- **Validation:** Perform all validation in service layer before database operations
- **Thread Safety:** Implement user-level locking to handle concurrent operations

### Potential Issues:
- In-memory tables have simulated latency (200-300ms) - tests will be slower
- No real database transactions - need manual consistency management
- HashMap/ArrayList are not thread-safe - need synchronization
- No persistence - data lost on restart (acceptable for practice project)

### Future Enhancements (Out of Scope):
- Maximum point limits
- Transaction reversal/refund functionality
- Batch operations
- Point expiration
- Real database integration
- Caching layer

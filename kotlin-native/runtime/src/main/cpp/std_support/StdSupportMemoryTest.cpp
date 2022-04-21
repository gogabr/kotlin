/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/Memory.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "KAssert.h"
#include "Utils.hpp"

using namespace kotlin;

namespace {

class EmptyClass {};

class Class {
public:
    explicit Class(int32_t x) : x_(x) {}

    int32_t x() const { return x_; }

private:
    int32_t x_;
};
static_assert(sizeof(Class) > sizeof(EmptyClass));

class ClassThrows {
public:
    explicit ClassThrows(int32_t x) : x_(x) { throw 13; }

    int32_t x() const { return x_; }

private:
    int32_t x_;
};

struct MockBaseAllocator {
    MOCK_METHOD(void*, allocate, (std::size_t), (noexcept));
    MOCK_METHOD(void, deallocate, (void*, std::size_t), (noexcept));
};

template <typename T>
struct MockAllocator {
    using value_type = T;
    using size_type = std::size_t;
    using difference_type = std::ptrdiff_t;
    using propagate_on_container_move_assignment = std::true_type;
    using is_always_equal = std::false_type;

    explicit MockAllocator(MockBaseAllocator& base) : base_(base) {}

    MockAllocator(const MockAllocator&) noexcept = default;

    template <typename U>
    MockAllocator(const MockAllocator<U>& other) noexcept : base_(other.base_) {}

    T* allocate(std::size_t n) noexcept { return static_cast<T*>(base_.allocate(sizeof(T) * n)); }

    void deallocate(T* p, std::size_t n) noexcept { base_.deallocate(p, sizeof(T) * n); }

    MockBaseAllocator& base_;
};

template <typename T, typename U>
bool operator==(const MockAllocator<T>& lhs, const MockAllocator<U>& rhs) noexcept {
    return &lhs.base_ == &rhs.base_;
}

template <typename T, typename U>
bool operator!=(const MockAllocator<T>& lhs, const MockAllocator<U>& rhs) noexcept {
    return !(lhs == rhs);
}

class MockClass : private Pinned {
public:
    class Mocker : private Pinned {
    public:
        Mocker() noexcept {
            RuntimeAssert(instance_ == nullptr, "Only one MockClass::Mocker at a time allowed");
            instance_ = this;
        }

        ~Mocker() {
            RuntimeAssert(instance_ == this, "MockClass::Mocker::instance_ is broken.");
            instance_ = nullptr;
        }

        MOCK_METHOD(void, ctor, (MockClass*, int));
        MOCK_METHOD(void, dtor, (MockClass*), (noexcept));

    private:
        friend class MockClass;

        static Mocker* instance_;
    };

    explicit MockClass(int x) { Mocker::instance_->ctor(this, x); }

    ~MockClass() noexcept { Mocker::instance_->dtor(this); }

    int32_t x() const { return x_; }

private:
    int32_t x_;
};
static_assert(sizeof(MockClass) > sizeof(EmptyClass));

// static
MockClass::Mocker* MockClass::Mocker::instance_ = nullptr;

} // namespace

TEST(StdSupportMemoryTest, Allocator) {
    using Allocator = std_support::allocator<Class>;
    using Traits = std::allocator_traits<Allocator>;
    Allocator a;
    Class* ptr = Traits::allocate(a, 1);
    new (ptr) Class(42);
    EXPECT_THAT(ptr->x(), 42);
    Traits::deallocate(a, ptr, 1);
}

TEST(StdSupportMemoryTest, AllocatorFromWrongClass) {
    using WrongClassAllocator = std_support::allocator<EmptyClass>;
    WrongClassAllocator base;
    using Allocator = typename std::allocator_traits<WrongClassAllocator>::template rebind_alloc<Class>;
    using Traits = typename std::allocator_traits<WrongClassAllocator>::template rebind_traits<Class>;
    Allocator a = Allocator(base);
    Class* ptr = Traits::allocate(a, 1);
    new (ptr) Class(42);
    EXPECT_THAT(ptr->x(), 42);
    Traits::deallocate(a, ptr, 1);
}

TEST(StdSupportMemoryTest, MakeUnique) {
    auto ptr = std_support::make_unique<Class>(42);
    EXPECT_THAT(ptr->x(), 42);
}

TEST(StdSupportMemoryTest, MakeUniqueThrows) {
    EXPECT_THROW(std_support::make_unique<ClassThrows>(42), int);
}

TEST(StdSupportMemoryTest, MakeShared) {
    auto ptr = std_support::make_shared<Class>(42);
    EXPECT_THAT(ptr->x(), 42);
}

TEST(StdSupportMemoryTest, MakeSharedThrows) {
    EXPECT_THROW(std_support::make_shared<ClassThrows>(42), int);
}

TEST(StdSupportMemoryTest, AllocatorNew) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocator, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto* ptr = std_support::allocator_new<MockClass>(MockAllocator<MockClass>(allocator), 42);
    EXPECT_THAT(ptr, expectedPtr);
}

TEST(StdSupportMemoryTest, AllocatorNewThrows) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocator, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42)).WillOnce([] { throw 17; });
        EXPECT_CALL(allocator, deallocate(expectedPtr, sizeof(MockClass)));
    }
    EXPECT_THROW(std_support::allocator_new<MockClass>(MockAllocator<MockClass>(allocator), 42), int);
}

TEST(StdSupportMemoryTest, AllocatorNewWrongType) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocator, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto* ptr = std_support::allocator_new<MockClass>(MockAllocator<EmptyClass>(allocator), 42);
    EXPECT_THAT(ptr, expectedPtr);
}

TEST(StdSupportMemoryTest, AllocatorDelete) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocator, deallocate(expectedPtr, sizeof(MockClass)));
    }
    std_support::allocator_delete(MockAllocator<MockClass>(allocator), expectedPtr);
}

TEST(StdSupportMemoryTest, AllocatorDeleteWrongType) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocator, deallocate(expectedPtr, sizeof(MockClass)));
    }
    std_support::allocator_delete(MockAllocator<EmptyClass>(allocator), expectedPtr);
}

TEST(StdSupportMemoryTest, AllocateUnique) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocator, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto ptr = std_support::allocate_unique<MockClass>(MockAllocator<MockClass>(allocator), 42);
    EXPECT_THAT(ptr.get(), expectedPtr);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocator, deallocate(expectedPtr, sizeof(MockClass)));
    }
    ptr.reset();
}

TEST(StdSupportMemoryTest, AllocatorUniqueThrows) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocator, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42)).WillOnce([] { throw 17; });
        EXPECT_CALL(allocator, deallocate(expectedPtr, sizeof(MockClass)));
    }
    EXPECT_THROW(std_support::allocate_unique<MockClass>(MockAllocator<MockClass>(allocator), 42), int);
}

TEST(StdSupportMemoryTest, AllocateUniqueWrongType) {
    testing::StrictMock<MockBaseAllocator> allocator;
    testing::StrictMock<MockClass::Mocker> mocker;

    MockClass* expectedPtr = reinterpret_cast<MockClass*>(13);

    {
        testing::InSequence s;
        EXPECT_CALL(allocator, allocate(sizeof(MockClass))).WillOnce(testing::Return(expectedPtr));
        EXPECT_CALL(mocker, ctor(expectedPtr, 42));
    }
    auto ptr = std_support::allocate_unique<MockClass>(MockAllocator<EmptyClass>(allocator), 42);
    EXPECT_THAT(ptr.get(), expectedPtr);

    {
        testing::InSequence s;
        EXPECT_CALL(mocker, dtor(expectedPtr));
        EXPECT_CALL(allocator, deallocate(expectedPtr, sizeof(MockClass)));
    }
    ptr.reset();
}

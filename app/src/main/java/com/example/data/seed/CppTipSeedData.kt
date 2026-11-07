package com.example.data.seed

import com.example.data.model.CppTip

object CppTipSeedData {
    fun getInitialTips(): List<CppTip> = listOf(
        CppTip(
            id = 1,
            title = "std::string_view: Non-owning String References",
            standard = "C++17",
            category = "Standard Library",
            summary = "Use std::string_view for read-only string parameters to eliminate unnecessary heap allocations and temporary copies.",
            content = """
                Passing `const std::string&` as a function parameter forces callers with string literals (`"hello"`) or `const char*` to construct a temporary `std::string`, often triggering dynamic memory allocation.

                `std::string_view` represents a non-owning pointer and length pair `[ptr, size]`. It can reference string literals, `std::string`, `char[]`, or slices of buffers with zero copying.

                ### Guidelines:
                1. Pass `std::string_view` by value, not by reference.
                2. Do not store `std::string_view` as a class member unless you guarantee the referenced string outlives the object.
                3. Beware of null termination: `std::string_view::data()` is NOT guaranteed to be null-terminated.
            """.trimIndent(),
            codeExample = """
                #include <iostream>
                #include <string_view>
                #include <string>

                // ✅ Good: takes string_view by value
                void print_prefix(std::string_view sv, std::size_t len) {
                    if (sv.length() >= len) {
                        std::cout << sv.substr(0, len) << '\n';
                    }
                }

                int main() {
                    // Zero allocations from string literal
                    print_prefix("Modern C++ Tip of the Week", 6);

                    // Zero allocations from existing std::string
                    std::string str = "Abseil & ISO C++";
                    print_prefix(str, 6);
                }
            """.trimIndent(),
            puzzleSnippet = """
                // What happens here? Is this safe?
                std::string_view get_view() {
                    std::string temp = "temporary string";
                    return temp; // ⚠️ Warning or UB?
                }
            """.trimIndent(),
            puzzleAnswer = "This returns a dangling string_view! The local std::string 'temp' is destroyed at the end of the function scope, leaving the returned view pointing to deallocated memory. Never return a string_view referencing a function-local string.",
            godboltUrl = "https://godbolt.org/z/xM37YhE7x",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/1.md",
            isFeatured = false
        ),
        CppTip(
            id = 11,
            title = "Return Value Optimization (RVO) & Copy Elision",
            standard = "C++17",
            category = "Performance & Memory",
            summary = "Guaranteed copy elision in C++17 ensures returning prvalues by value creates zero copies or moves. Do not use std::move on returns!",
            content = """
                Prior to C++17, returning a local object by value relied on the compiler's optional RVO / NRVO. In C++17, returning a prvalue (pure rvalue) is guaranteed to elide copies and moves completely.

                A frequent anti-pattern is writing `return std::move(obj);`. This actively prevents NRVO because `std::move` casts the expression to an xvalue (rvalue reference), turning what could have been constructed directly in the caller's memory into an explicit move operation!
            """.trimIndent(),
            codeExample = """
                #include <vector>
                #include <string>
                #include <utility>

                struct BigData {
                    std::vector<int> buffer{1000000, 42};
                };

                // ✅ Best: NRVO allows constructing in-place in caller
                BigData create_data() {
                    BigData data;
                    // populate data...
                    return data; // NRVO applied!
                }

                // ❌ Anti-pattern: forces an unnecessary move and disables NRVO
                BigData bad_create_data() {
                    BigData data;
                    return std::move(data); // Don't do this!
                }
            """.trimIndent(),
            puzzleSnippet = """
                struct Tracker {
                    Tracker() = default;
                    Tracker(const Tracker&) { std::cout << "Copy\n"; }
                    Tracker(Tracker&&) noexcept { std::cout << "Move\n"; }
                };

                Tracker make() {
                    return Tracker(); // What gets printed in C++17+?
                }
            """.trimIndent(),
            puzzleAnswer = "Nothing is printed! In C++17 and later, prvalue return copy elision is mandatory by the language specification. Neither the copy nor the move constructor is ever called.",
            godboltUrl = "https://godbolt.org/z/q3bcsTj1n",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/11.md",
            isFeatured = false
        ),
        CppTip(
            id = 77,
            title = "Explicit Types vs. auto: The AAA Rule",
            standard = "C++14",
            category = "Modern Idioms",
            summary = "Use auto when the type is redundant, unutterable (lambdas), or implementation-defined; be explicit when type conversion is intended.",
            content = """
                Herb Sutter popularised 'Almost Always Auto' (AAA). However, Google and TotW guidelines recommend a nuanced balance:

                1. Use `auto` when the type is repeated: `auto ptr = std::make_unique<Widget>();`
                2. Use `auto` for iterators and complex template instantiations where spelling the type obscures intent.
                3. Spell out the type explicitly when you want to enforce a specific numeric precision (e.g., `uint32_t`, `double`) or when an implicit conversion is desired.
            """.trimIndent(),
            codeExample = """
                #include <memory>
                #include <vector>
                #include <map>

                void example() {
                    // ✅ Good: type is immediately clear from RHS
                    auto widget = std::make_unique<std::vector<int>>();
                    
                    // ✅ Good: iterator type is verbose and distracting
                    std::map<std::string, int> scores;
                    for (const auto& [name, score] : scores) {
                        // Structured binding with auto
                    }

                    // ⚠️ Be careful: float vs double precision
                    float f = 1.0; // explicit float
                    auto d = 1.0;  // double by default!
                }
            """.trimIndent(),
            puzzleSnippet = """
                std::vector<bool> v = {true, false, true};
                auto val = v[0]; // What type is 'val'? Is it bool?
            """.trimIndent(),
            puzzleAnswer = "No! 'val' is std::vector<bool>::reference, a proxy object. If the vector is modified or goes out of scope, holding onto 'val' can lead to undefined behavior. Use 'bool val = v[0];' when you want the actual boolean value.",
            godboltUrl = "https://godbolt.org/z/W6zY8b53E",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/77.md",
            isFeatured = false
        ),
        CppTip(
            id = 107,
            title = "std::as_const: Expressive Const References",
            standard = "C++17",
            category = "Standard Library",
            summary = "Use std::as_const to obtain a const reference to an lvalue, avoiding ugly and error-prone const_cast or static_cast syntax.",
            content = """
                Occasionally you need to invoke the `const` overload of a member function or ensure that a template binds to `const T&`. 
                Historically, developers wrote `static_cast<const T&>(obj)`.

                `std::as_const` (in `<utility>`) provides a clean, self-documenting syntax. It is also safe because passing an rvalue is deleted to prevent dangling references!
            """.trimIndent(),
            codeExample = """
                #include <utility>
                #include <vector>
                #include <iostream>

                struct DataHolder {
                    int* data() {
                        std::cout << "non-const data()\n";
                        return nullptr;
                    }
                    const int* data() const {
                        std::cout << "const data()\n";
                        return nullptr;
                    }
                };

                int main() {
                    DataHolder holder;
                    holder.data(); // non-const

                    // ✅ Clear, concise, safe:
                    std::as_const(holder).data(); // const data()
                }
            """.trimIndent(),
            puzzleSnippet = """
                // Will this compile?
                auto&& r = std::as_const(std::string("hello"));
            """.trimIndent(),
            puzzleAnswer = "It fails to compile! std::as_const has a deleted overload for rvalues (std::as_const(const T&&) = delete) specifically designed to prevent binding temporary rvalues to dangling const references.",
            godboltUrl = "https://godbolt.org/z/69qG8qP6d",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/107.md",
            isFeatured = false
        ),
        CppTip(
            id = 112,
            title = "emplace_back vs. push_back: Choose with Intent",
            standard = "C++11",
            category = "Standard Library",
            summary = "emplace_back constructs objects in-place from constructor arguments; push_back inserts existing objects. Don't blindly replace push_back with emplace_back.",
            content = """
                Many developers assume `emplace_back` is strictly a faster drop-in replacement for `push_back`.
                However:
                1. `emplace_back` calls `explicit` constructors because it uses direct initialization. This can mask unintended conversions (like `v.emplace_back(100)` creating a vector with 100 empty elements rather than adding integer 100).
                2. When you already have an object, `push_back(std::move(x))` is identical in performance and clarifies intent.
                3. Reserve `emplace_back` for when you are passing multiple constructor arguments to construct the element in-place.
            """.trimIndent(),
            codeExample = """
                #include <vector>
                #include <string>

                struct Item {
                    Item(int id, std::string name) {}
                };

                int main() {
                    std::vector<Item> items;

                    // ✅ Great use of emplace_back: in-place construction
                    items.emplace_back(42, "C++ Tip");

                    Item readyItem{10, "Existing"};
                    // ✅ Great use of push_back: moving already existing object
                    items.push_back(std::move(readyItem));
                }
            """.trimIndent(),
            puzzleSnippet = """
                std::vector<std::vector<int>> matrix;
                // What happens with these two?
                matrix.push_back(10);    // (A)
                matrix.emplace_back(10); // (B)
            """.trimIndent(),
            puzzleAnswer = "(A) Fails to compile because std::vector(size_t) is explicit. (B) Compiles and creates a vector of 10 zeros inside the matrix! This demonstrates how emplace_back can bypass explicit constructor safeguards.",
            godboltUrl = "https://godbolt.org/z/8zfej6Ecf",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/112.md",
            isFeatured = false
        ),
        CppTip(
            id = 140,
            title = "Deducing this: Explicit Object Parameter in C++23",
            standard = "C++23",
            category = "Modern Idioms",
            summary = "C++23 deducing this eliminates const and ref-qualifier boilerplate, enables recursive lambdas, and simplifies CRTP.",
            content = """
                Before C++23, writing member functions that return references based on the caller's constness and value category required duplicating code up to 4 times (`&`, `const&`, `&&`, `const&&`).

                With C++23's explicit object parameter syntax `this auto&& self`, the cv-qualifiers and value category of the calling object are deduced in a single function template!

                It also unlocks:
                - Elegant recursive lambdas without `std::function`.
                - Simplified Curiously Recurring Template Pattern (CRTP) without inheriting from base templates.
            """.trimIndent(),
            codeExample = """
                #include <utility>
                #include <iostream>

                struct Node {
                    int value = 42;

                    // ✅ One function handles const Node&, Node&, and Node&&
                    template <typename Self>
                    auto&& get(this Self&& self) {
                        return std::forward<Self>(self).value;
                    }
                };

                // Recursive lambda using 'this self'
                auto gcd = [](this auto self, int a, int b) -> int {
                    return b == 0 ? a : self(b, a % b);
                };
            """.trimIndent(),
            puzzleSnippet = """
                // How does CRTP become simpler with deducing this?
                struct Printable {
                    void print(this const auto& self) {
                        std::cout << self.to_string() << '\n';
                    }
                };
                struct User : Printable {
                    std::string to_string() const { return "Alice"; }
                };
            """.trimIndent(),
            puzzleAnswer = "No template parameter is needed on the base class! Printable no longer needs to be 'template <typename Derived> struct Printable'. It accesses the derived class directly through the deduced 'self' parameter.",
            godboltUrl = "https://godbolt.org/z/oEee4eK3x",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/140.md",
            isFeatured = true
        ),
        CppTip(
            id = 148,
            title = "std::format: Fast, Type-Safe String Formatting",
            standard = "C++20",
            category = "Standard Library",
            summary = "std::format replaces printf's lack of type-safety and iostreams' verbose formatting with Python-style braces and compile-time format checks.",
            content = """
                C++20 introduced `<format>`, based on Victor Zverovich's popular `{fmt}` library.
                
                Advantages over `std::stringstream`:
                - Up to 3-5x faster runtime performance.
                - Significantly smaller binary size footprint.
                - Compile-time format string validation (in C++20/C++23 via `consteval`).
                - Locales are opt-in, avoiding accidental locale-dependent number formatting bugs.
            """.trimIndent(),
            codeExample = """
                #include <format>
                #include <string>
                #include <iostream>

                int main() {
                    std::string name = "Bjarne";
                    int year = 1985;

                    // Clean and typesafe
                    std::string msg = std::format("{} released C++ in {:04d}!", name, year);
                    std::cout << msg << '\n';

                    // Number formatting: hex, precision, alignment
                    double pi = 3.14159265;
                    std::cout << std::format("Pi: {:.2f}, Hex: {:#x}\n", pi, 255);
                }
            """.trimIndent(),
            puzzleSnippet = """
                // Will this compile in C++20?
                // std::format("{:d}", "string_arg");
            """.trimIndent(),
            puzzleAnswer = "Compilation error! In C++20, std::format uses consteval format strings. Passing an invalid type specifier (such as ':d' for a string argument) fails at compile time, completely preventing printf-style runtime crashes.",
            godboltUrl = "https://godbolt.org/z/6P9re73s6",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/148.md",
            isFeatured = false
        ),
        CppTip(
            id = 180,
            title = "Avoid std::endl: Prefer '\\n'",
            standard = "C++11",
            category = "Performance & Memory",
            summary = "std::endl writes a newline AND flushes the stream. In loops or fast I/O, this degrades performance by orders of magnitude.",
            content = """
                Many introductory C++ tutorials teach `std::cout << "..." << std::endl;`.
                
                What `std::endl` actually does:
                ```cpp
                os.put('\n');
                os.flush();
                ```
                Flushing the stream forces an expensive OS syscall and blocks program execution until the buffer is cleared. Streams are already buffered for a reason.

                Only flush when you genuinely need immediate output (e.g., interactive prompt before `std::cin` or logging right before a crash). For general printing, always write `\n`.
            """.trimIndent(),
            codeExample = """
                #include <iostream>

                void fast_output() {
                    // ✅ Good: buffered, fast
                    for (int i = 0; i < 1000; ++i) {
                        std::cout << i << '\n';
                    }
                    // std::cout automatically flushes upon termination or std::cin read
                }
            """.trimIndent(),
            puzzleSnippet = """
                // Does std::cin require an explicit std::endl on std::cout?
                std::cout << "Enter name: ";
                std::cin >> name;
            """.trimIndent(),
            puzzleAnswer = "No! std::cin is automatically 'tied' to std::cout (std::cin.tie(&std::cout)). Any read from std::cin automatically flushes std::cout before reading user input.",
            godboltUrl = "https://godbolt.org/z/f78Yvj164",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/180.md",
            isFeatured = false
        ),
        CppTip(
            id = 182,
            title = "Initialize Your Ints! Prevent Undefined Behavior",
            standard = "C++11",
            category = "Core Language",
            summary = "Local variables of built-in fundamental types are default-initialized to indeterminate garbage. Always initialize them.",
            content = """
                Unlike languages like Java or C#, C++ does not zero-initialize local primitive variables by default for zero-overhead performance.

                Reading from an uninitialized scalar variable is undefined behavior (UB). Compilers may assume UB never happens, leading to aggressive and counterintuitive optimizations (e.g. deleting surrounding branches).

                ### Best Practices:
                - `int count = 0;` or `int count{0};`
                - Even better: declare variables only when you have their initial value ready (`const int count = calculate();`).
            """.trimIndent(),
            codeExample = """
                #include <iostream>

                void bad() {
                    int x; // ⚠️ Indeterminate value!
                    if (x > 0) { // UB! Compiler can assume anything
                        std::cout << "Positive\n";
                    }
                }

                void good() {
                    int x = 0;   // ✅ Safe
                    auto y = 42;  // ✅ Deduce and initialize
                }
            """.trimIndent(),
            puzzleSnippet = """
                struct S {
                    int a;
                    int b;
                };

                S s1{};       // What are s1.a and s1.b?
                S s2;         // What are s2.a and s2.b?
            """.trimIndent(),
            puzzleAnswer = "s1{} performs value-initialization: both s1.a and s1.b are guaranteed to be 0! But 'S s2;' default-initializes them to indeterminate values. Using {} is an easy way to ensure aggregate zero-initialization.",
            godboltUrl = "https://godbolt.org/z/7rWP31P4Y",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/182.md",
            isFeatured = false
        ),
        CppTip(
            id = 200,
            title = "Ranges and std::views: Composable Algorithms",
            standard = "C++20",
            category = "Standard Library",
            summary = "std::views lets you filter, transform, and slice data pipelines lazily without allocating intermediate temporary vectors.",
            content = """
                Classic `<algorithm>` operations like `std::transform` and `std::copy_if` required passing `begin()` and `end()` iterators and allocating intermediate collections for chained operations.

                C++20 Ranges introduces `std::views` with pipe syntax (`|`). Views are lightweight non-owning objects that evaluate items lazily on demand with O(1) construction.
            """.trimIndent(),
            codeExample = """
                #include <iostream>
                #include <vector>
                #include <ranges>

                int main() {
                    std::vector<int> numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

                    // Lazy pipeline: filter evens, square them, take first 3
                    auto results = numbers 
                        | std::views::filter([](int n) { return n % 2 == 0; })
                        | std::views::transform([](int n) { return n * n; })
                        | std::views::take(3);

                    for (int n : results) {
                        std::cout << n << ' '; // Prints: 4 16 36
                    }
                }
            """.trimIndent(),
            puzzleSnippet = """
                auto v = std::views::iota(1) | std::views::take(5);
                // Does this allocate any memory on the heap?
            """.trimIndent(),
            puzzleAnswer = "Zero heap allocations! std::views::iota generates numbers lazily, and std::views::take creates a view adaptor that stops iterating after 5 steps. Everything is stack-allocated and optimized by the compiler.",
            godboltUrl = "https://godbolt.org/z/Kj6c31e41",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/200.md",
            isFeatured = false
        ),
        CppTip(
            id = 223,
            title = "Designated Initializers: Explicit Struct Fields",
            standard = "C++20",
            category = "Core Language",
            summary = "C++20 designated initializers make aggregate construction self-documenting, preventing subtle argument reordering bugs.",
            content = """
                When configuring structs with multiple fields of the same type (e.g., width, height, margin), traditional positional initialization `{10, 20, 5}` easily leads to swapped argument bugs.

                C++20 designated initializers allow naming fields explicitly:
                `Config{.width = 10, .height = 20}`.

                ### C++ Rules (differs slightly from C):
                1. Designators must appear in the exact order members are declared in the struct.
                2. Designators cannot be nested directly in the outer syntax.
                3. Omitted fields are initialized to their default values (or value-initialized to 0).
            """.trimIndent(),
            codeExample = """
                struct WindowOptions {
                    int width = 800;
                    int height = 600;
                    bool fullscreen = false;
                    bool vsync = true;
                };

                int main() {
                    // ✅ Self-explanatory and safe
                    WindowOptions opts{
                        .width = 1920,
                        .height = 1080,
                        .fullscreen = true
                        // vsync defaults to true
                    };
                }
            """.trimIndent(),
            puzzleSnippet = """
                struct Point { int x; int y; };
                // Is this valid in C++20?
                Point p{.y = 20, .x = 10};
            """.trimIndent(),
            puzzleAnswer = "No! In C++, designated initializers must follow the declaration order of member variables. In C it is allowed out of order, but in C++ you must write {.x = 10, .y = 20} to prevent confusing side-effects.",
            godboltUrl = "https://godbolt.org/z/M57eEv58P",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/223.md",
            isFeatured = false
        ),
        CppTip(
            id = 254,
            title = "Static Reflection: The Future of Metaprogramming",
            standard = "C++26",
            category = "Templates & Metaprogramming",
            summary = "C++26 static reflection allows inspecting types, enum names, and struct members at compile-time with zero macro hacks.",
            content = """
                For decades, serialization, hashing, and enum-to-string conversions in C++ required macros, external code generators, or template boilerplate.

                The static reflection proposal (P2996) accepted into C++26 brings first-class reflection:
                - `^^T` operator produces a `std::meta::info` reflection token.
                - Reflection functions in `std::meta` inspect member names, types, and annotations.
                - Splicers `[: ... :]` inject reflected entities back into code.
            """.trimIndent(),
            codeExample = """
                #include <experimental/meta>
                #include <iostream>

                enum class Color { Red, Green, Blue };

                // Future C++26 reflection pattern
                template <typename Enum>
                void print_enum_names() {
                    // Using C++26 static reflection
                    constexpr auto members = std::meta::enumerators_of(^^Enum);
                    template for (constexpr auto member : members) {
                        std::cout << std::meta::identifier_of(member) << '\n';
                    }
                }
            """.trimIndent(),
            puzzleSnippet = """
                // What is the type of ^^int in the C++26 proposal?
            """.trimIndent(),
            puzzleAnswer = "The type is std::meta::info. It is an opaque compile-time value representing a reflection reflection descriptor of the entity.",
            godboltUrl = "https://godbolt.org/z/6E5rYj8qv",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/254.md",
            isFeatured = false
        ),
        CppTip(
            id = 268,
            title = "std::span: Contiguous Sequences Without Ownership",
            standard = "C++20",
            category = "Standard Library",
            summary = "std::span provides bounds-checked or raw views over arrays, vectors, and buffers without heap allocations or template bloat.",
            content = """
                Functions that accept an array of data traditionally accepted `(T* ptr, size_t size)` or had to template over `std::vector<T>`, `std::array<T, N>`, etc.

                `std::span<T>` unifies all contiguous memory containers into one non-owning view. Unlike `std::string_view` which is for characters, `std::span` works on any type and allows modifying elements if `T` is non-const!
            """.trimIndent(),
            codeExample = """
                #include <span>
                #include <vector>
                #include <array>
                #include <numeric>

                int sum(std::span<const int> data) {
                    return std::accumulate(data.begin(), data.end(), 0);
                }

                int main() {
                    std::vector<int> v = {1, 2, 3};
                    std::array<int, 3> a = {4, 5, 6};
                    int raw[] = {7, 8, 9};

                    // Works seamlessly on all three without copies!
                    sum(v);
                    sum(a);
                    sum(raw);
                }
            """.trimIndent(),
            puzzleSnippet = """
                // Can std::span change the size of the underlying container?
            """.trimIndent(),
            puzzleAnswer = "No. std::span is purely a view over existing memory. It cannot reallocate, push, or pop elements in the original container.",
            godboltUrl = "https://godbolt.org/z/Kx9M39fbb",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/268.md",
            isFeatured = false
        ),
        CppTip(
            id = 285,
            title = "std::expected: Functional Error Handling",
            standard = "C++23",
            category = "Standard Library",
            summary = "std::expected<T, E> stores either a valid value of type T or an error of type E without exception overhead.",
            content = """
                Exceptions can be unsuitable in real-time, low-latency, or embedded environments. Returning status codes with output parameters is verbose and prone to ignored errors.

                `std::expected<T, E>` provides a type-safe union with monadic operations (`and_then`, `or_else`, `transform`). Callers are forced to handle the possibility of failure.
            """.trimIndent(),
            codeExample = """
                #include <expected>
                #include <string>
                #include <iostream>

                enum class MathError { DivisionByZero, NegativeRoot };

                std::expected<double, MathError> divide(double a, double b) {
                    if (b == 0.0) return std::unexpected(MathError::DivisionByZero);
                    return a / b;
                }

                int main() {
                    auto result = divide(10.0, 2.0);
                    if (result) {
                        std::cout << "Value: " << *result << '\n';
                    } else {
                        std::cout << "Error occurred!\n";
                    }
                }
            """.trimIndent(),
            puzzleSnippet = """
                // What happens if you call result.value() when it holds an error?
            """.trimIndent(),
            puzzleAnswer = "It throws std::bad_expected_access<E>(error). If you don't want exceptions, inspect 'result.has_value()' or use '*result' with caution, or provide a default using 'value_or()'.",
            godboltUrl = "https://godbolt.org/z/q9f9re9W9",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/285.md",
            isFeatured = false
        ),
        CppTip(
            id = 299,
            title = "Concepts: Constrained Templates Over SFINAE",
            standard = "C++20",
            category = "Templates & Metaprogramming",
            summary = "C++20 concepts replace obscure std::enable_if and SFINAE tricks with clear, expressive syntax and human-readable compiler errors.",
            content = """
                Before C++20, constraining templates required `std::enable_if_t`, causing cryptic error messages when a constraint failed.

                Concepts specify requirements on template arguments at compile time:
                - `template <std::integral T>`
                - `requires` clause: `requires requires(T a) { a.hash(); }`
                - Terse syntax: `void sort(std::sortable auto& range);`
            """.trimIndent(),
            codeExample = """
                #include <concepts>
                #include <iostream>

                // Define a custom concept
                template <typename T>
                concept Printable = requires(T x) {
                    { std::cout << x } -> std::same_as<std::ostream&>;
                };

                // Constrain function using concept
                void print_value(Printable auto val) {
                    std::cout << val << '\n';
                }

                int main() {
                    print_value(42);       // ✅ Compiles
                    print_value("C++20");  // ✅ Compiles
                    // struct Empty{}; print_value(Empty{}); // ❌ Clean error: constraint not satisfied!
                }
            """.trimIndent(),
            puzzleSnippet = """
                template <typename T>
                requires std::integral<T> || std::floating_point<T>
                T add(T a, T b) { return a + b; }
                // What happens if you pass a std::string?
            """.trimIndent(),
            puzzleAnswer = "The compiler gives a clear error message stating that neither std::integral<std::string> nor std::floating_point<std::string> was satisfied, pinpointing the failed concept immediately.",
            godboltUrl = "https://godbolt.org/z/4Mzv5G1Gj",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/299.md",
            isFeatured = false
        ),
        CppTip(
            id = 301,
            title = "constexpr to_chars and from_chars in C++23",
            standard = "C++23",
            category = "Core Language",
            summary = "std::to_chars and std::from_chars for integral types are fully constexpr in C++23, enabling compile-time string conversions.",
            content = """
                Fast, locale-independent integer-to-string and string-to-integer conversions with `<charconv>` were introduced in C++17.

                Starting in C++23, their integral overloads are declared `constexpr`. You can now serialize integers to character buffers and parse numbers from text entirely during compilation with `static_assert`!
            """.trimIndent(),
            codeExample = """
                #include <charconv>
                #include <array>
                #include <string_view>

                consteval int parse_number_at_compile_time(std::string_view sv) {
                    int val = 0;
                    std::from_chars(sv.data(), sv.data() + sv.size(), val);
                    return val;
                }

                int main() {
                    // Evaluated entirely by the compiler
                    constexpr int value = parse_number_at_compile_time("2023");
                    static_assert(value == 2023);
                }
            """.trimIndent(),
            puzzleSnippet = """
                // Can std::to_chars format floats at compile time in C++23?
            """.trimIndent(),
            puzzleAnswer = "In C++23, only integral overloads of to_chars/from_chars are constexpr. Floating-point constexpr support is slated for future standard revisions due to the complexity of Ryu/Dragon4 floating-point algorithms.",
            godboltUrl = "https://godbolt.org/z/jP79K1M3h",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/301.md",
            isFeatured = false
        ),
        CppTip(
            id = 312,
            title = "std::move Doesn't Move, std::forward Doesn't Forward",
            standard = "C++11",
            category = "Core Language",
            summary = "Both std::move and std::forward are purely compile-time casts; neither generates a single byte of executable machine code.",
            content = """
                A common misconception among developers is that `std::move` moves data or modifies memory.

                In reality:
                - `std::move(x)` is unconditionally `static_cast<std::remove_reference_t<T>&&>(x)`. It converts an lvalue into an rvalue so that a move constructor or move assignment operator can be selected during overload resolution.
                - `std::forward<T>(x)` is a conditional cast: it casts to an rvalue only if `T` was passed as an rvalue, preserving value category in generic forwarding references (`T&&`).
            """.trimIndent(),
            codeExample = """
                #include <utility>
                #include <string>

                void process(std::string&& s) {
                    // s has a name, so 's' itself is an lvalue!
                    // To forward it to another rvalue sink, move it again:
                    std::string sink = std::move(s);
                }

                template <typename T>
                void forward_wrapper(T&& arg) {
                    // Preserves lvalueness or rvalueness
                    process(std::forward<T>(arg));
                }
            """.trimIndent(),
            puzzleSnippet = """
                const std::string s = "immutable";
                std::string dest = std::move(s);
                // Was 's' moved from?
            """.trimIndent(),
            puzzleAnswer = "No! Because 's' is const, std::move(s) evaluates to 'const std::string&&'. The move constructor takes 'std::string&&' (non-const), so overload resolution silently falls back to the copy constructor! Always ensure objects to be moved are non-const.",
            godboltUrl = "https://godbolt.org/z/q7T1T78bv",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/312.md",
            isFeatured = false
        ),
        CppTip(
            id = 325,
            title = "std::jthread: Cooperative Interruption & Auto-Join",
            standard = "C++20",
            category = "Concurrency",
            summary = "std::jthread automatically joins upon destruction and supports cooperative thread cancellation via std::stop_token.",
            content = """
                With `std::thread`, forgetting to call `.join()` or `.detach()` before the destructor runs causes `std::terminate()` to abort the process.

                `std::jthread` in C++20 fixes this:
                1. Auto-joining: the destructor automatically calls `request_stop()` and `join()`.
                2. Cooperative cancellation: passes a `std::stop_token` to the thread function so long-running loops can poll `stop_token.stop_requested()`.
            """.trimIndent(),
            codeExample = """
                #include <thread>
                #include <chrono>
                #include <iostream>

                void worker(std::stop_token stoken) {
                    while (!stoken.stop_requested()) {
                        std::cout << "Working...\n";
                        std::this_thread::sleep_for(std::chrono::milliseconds(100));
                    }
                    std::cout << "Cleanly stopped.\n";
                }

                int main() {
                    {
                        std::jthread jt(worker);
                        std::this_thread::sleep_for(std::chrono::milliseconds(300));
                        // Destructor of jt runs here: requests stop and joins automatically!
                    }
                    std::cout << "Worker joined.\n";
                }
            """.trimIndent(),
            puzzleSnippet = """
                // Does calling jthread::request_stop() forcefully kill the thread?
            """.trimIndent(),
            puzzleAnswer = "No! It is strictly cooperative. It sets a flag that the thread must actively check using 'stoken.stop_requested()'. This prevents resource leaks and corrupted locks.",
            godboltUrl = "https://godbolt.org/z/19Kvd9b4b",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/325.md",
            isFeatured = false
        ),
        CppTip(
            id = 349,
            title = "C++26 SI Prefixes for std::chrono and Units",
            standard = "C++26",
            category = "Standard Library",
            summary = "C++26 introduces new SI metric prefixes (quecto, ronto, ronna, quetta) to standard units, expanding high-precision science support.",
            content = """
                In 2022, the International Bureau of Weights and Measures (BIPM) expanded the SI metric prefix system with:
                - `ronna` ($10^{27}$), `quetta` ($10^{30}$)
                - `ronto` ($10^{-27}$), `quecto` ($10^{-30}$)

                Tip of the week highlights how modern C++ updates `<ratio>` and `<chrono>` to adopt international scientific standards for astrophysics and subatomic measurements.
            """.trimIndent(),
            codeExample = """
                #include <ratio>
                #include <chrono>
                #include <iostream>

                int main() {
                    // C++26 ratio definitions
                    using Quetta = std::ratio<1000000000000000000000000000000LL, 1>;
                    // std::chrono::duration supports extreme cosmic time scales
                    std::cout << "SI Prefixes supported in C++26 standard ratios\n";
                }
            """.trimIndent(),
            puzzleSnippet = """
                // What ratio represents 1 nanosecond in std::chrono?
            """.trimIndent(),
            puzzleAnswer = "std::nano, which is std::ratio<1, 1000000000> (10^-9 seconds).",
            godboltUrl = "https://godbolt.org/z/M5G9r4f1K",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/349.md",
            isFeatured = false
        ),
        CppTip(
            id = 362,
            title = "Pack Indexing in C++26: args...[N]",
            standard = "C++26",
            category = "Templates & Metaprogramming",
            summary = "C++26 introduces direct pack indexing args...[0], eliminating recursive template instantiations and std::tuple conversions.",
            content = """
                Retrieving the N-th element from a variadic template parameter pack historically required:
                1. Converting the pack to a `std::tuple` and using `std::get<N>`.
                2. Writing recursive helper structs with O(N) template instantiations.

                C++26 introduces direct pack indexing syntax:
                `args...[i]` (for pack elements) and `Ts...[i]` (for pack types)!
            """.trimIndent(),
            codeExample = """
                #include <iostream>

                template <typename... Ts>
                void inspect_first_and_last(Ts... args) {
                    // C++26 Pack Indexing
                    constexpr auto size = sizeof...(args);
                    if constexpr (size > 0) {
                        std::cout << "First: " << args...[0] << '\n';
                        std::cout << "Last:  " << args...[size - 1] << '\n';
                    }
                }

                int main() {
                    inspect_first_and_last(1, "hello", 3.14);
                }
            """.trimIndent(),
            puzzleSnippet = """
                // How do you access the N-th TYPE in a parameter pack in C++26?
                // Ts...[0] or Ts[0]?
            """.trimIndent(),
            puzzleAnswer = "Ts...[0]! The ellipsis comes before the bracket: 'using FirstType = Ts...[0];'.",
            godboltUrl = "https://godbolt.org/z/Y6z6c4o1M",
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/362.md",
            isFeatured = false
        )
    )

    fun getCatalogTips(): List<CppTip> = listOf(
        createCatalogTip(187, "Raw string literals R\"(...)\" for Regex and JSON", "C++11", "Core Language"),
        createCatalogTip(194, "Bit-fields with Default Member Initializers", "C++20", "Core Language"),
        createCatalogTip(206, "std::type_identity for Disabling Argument Deduction", "C++20", "Templates & Metaprogramming"),
        createCatalogTip(212, "std::source_location for Modern Logging", "C++20", "Standard Library"),
        createCatalogTip(215, "Lambdas and Captured Values: Lifetime Pitfalls", "C++14", "Modern Idioms"),
        createCatalogTip(228, "std::midpoint and std::lerp for Numeric Precision", "C++20", "Standard Library"),
        createCatalogTip(232, "std::bit_cast for Fast Type Punning", "C++20", "Performance & Memory"),
        createCatalogTip(237, "Lambdas in Unevaluated Contexts (decltype)", "C++20", "Templates & Metaprogramming"),
        createCatalogTip(241, "Concepts vs constexpr if for Template Selection", "C++20", "Templates & Metaprogramming"),
        createCatalogTip(246, "std::invoke and Callable Objects", "C++17", "Standard Library"),
        createCatalogTip(250, "The [[assume(expr)]] Attribute for Compiler Optimization", "C++23", "Performance & Memory"),
        createCatalogTip(252, "Constexpr Virtual Member Functions", "C++20", "Core Language"),
        createCatalogTip(258, "std::views::enumerate for Clean Index-based Loops", "C++23", "Standard Library"),
        createCatalogTip(262, "std::views::zip and zip_transform", "C++23", "Standard Library"),
        createCatalogTip(274, "std::stacktrace for Crash Diagnostics", "C++23", "Standard Library"),
        createCatalogTip(280, "std::unreachable() for Dead Branch Elimination", "C++23", "Performance & Memory"),
        createCatalogTip(290, "std::generator and C++20 Coroutines", "C++23", "Concurrency"),
        createCatalogTip(295, "std::print and std::println Formatted Output", "C++23", "Standard Library"),
        createCatalogTip(305, "Explicit Object Parameter / Deducing this", "C++23", "Modern Idioms"),
        createCatalogTip(310, "std::flat_set and std::flat_map Cache-friendly Containers", "C++23", "Performance & Memory"),
        createCatalogTip(318, "std::mdspan Multi-Dimensional Array View", "C++23", "Standard Library"),
        createCatalogTip(330, "Constexpr std::unique_ptr in Modern C++", "C++20", "Standard Library"),
        createCatalogTip(335, "Constexpr std::vector and std::string", "C++20", "Standard Library"),
        createCatalogTip(338, "Consteval Immediate Functions", "C++20", "Core Language"),
        createCatalogTip(342, "std::views::chunk and chunk_by Ranges", "C++23", "Standard Library"),
        createCatalogTip(345, "std::views::slide and stride Windows", "C++23", "Standard Library"),
        createCatalogTip(354, "std::views::cartesian_product", "C++23", "Standard Library"),
        createCatalogTip(358, "Template for-loop and Static Reflection", "C++26", "Templates & Metaprogramming"),
        createCatalogTip(366, "Structured Bindings with Pack Indexing", "C++26", "Templates & Metaprogramming"),
        createCatalogTip(370, "Pattern Matching in C++ (inspect / is)", "C++26", "Modern Idioms"),
        createCatalogTip(374, "User-generated static_assert Messages in C++26", "C++26", "Core Language"),
        createCatalogTip(378, "std::inplace_vector for Fixed-capacity Allocation-free Vector", "C++26", "Performance & Memory"),
        createCatalogTip(382, "Linear Algebra with std::linalg", "C++26", "Standard Library"),
        createCatalogTip(385, "Contracts in C++: Preconditions and Postconditions", "C++26", "Core Language")
    )

    fun getAllSeedTips(): List<CppTip> = getInitialTips() + getCatalogTips()

    private fun createCatalogTip(id: Int, title: String, standard: String, category: String): CppTip {
        return CppTip(
            id = id,
            title = title,
            standard = standard,
            category = category,
            summary = "Tip #$id from tip-of-the-week/cpp. Tap to read and sync full markdown and code example from GitHub.",
            content = "This tip is in the catalog index. When connected, tap 'Download full tip' or open it to fetch the full text and code snippet from GitHub.",
            codeExample = "// Tip #$id from tip-of-the-week/cpp\n// Tap 'Download Tip' to fetch full code and puzzle.",
            puzzleSnippet = null,
            puzzleAnswer = null,
            godboltUrl = null,
            githubUrl = "https://github.com/tip-of-the-week/cpp/blob/master/tips/$id.md",
            isBookmarked = false,
            isFeatured = false,
            isDownloaded = false
        )
    }
}

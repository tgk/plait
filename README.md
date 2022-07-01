# plait

`plait` is a small macro that works like a `let` binding but where
nested `plait`s allows for previously defined bindings to be
overwritten.

In practice, the combined list of nested `let` bindings are combined
to one new `let` statement which means that any side-effects will also
be re-evaluated.

Here is a short example taken from the test file of this project:

```clj
(use 'plait.core)

(plait [a 42
        b (inc a)]
  (println b) ;; prints 43
  (plait [b (+ 2 a)]
    (println b)) ;; prints 44
  (plait [a 10]
    (println b))) ;; prints 11
```

The `plait` macro rewrites the above expression to:

```clj
(let [a 42
      b (inc a)]
  (println b) ;; prints 43
  (let [a 42
        b (+ 2 a)]
    (println b)) ;; prints 44
  (let [a 10
        b (inc a)]
    (println b))) ;; prints 11
```

## Rationale

At [GoMore](https://gomore.dk) we found ourselves writing many tests
where only a small change in a database object was needed to test a
new assertion and we would often write code like this:

```clj
(testing "car owner is a business owner"
  (let [rental-ad (create :rental-ad {:ownership :business})
        renter    (create :user)
        rental    (create :rental    {:rental-ad rental-ad
                                      :renter    user})
        response  (get-rental-details renter rental)]
    (is (= 2 (:vat-rate response)))))
(testing "car owner is a private owner"
  (let [rental-ad (create :rental-ad {:ownership :private})
        renter    (create :user)
        rental    (create :rental    {:rental-ad rental-ad
                                      :renter    user})
        response  (get-rental-details renter rental)]
    (is (= 1 (:vat-rate response)))))
```

which very quickly becomes difficult both to read and maintain.

With `plait` the above tests can be rewritten to:

```clj
(plait [ownership nil
        rental-ad (create :rental-ad {:ownership ownership})
        renter    (create :user)
        rental    (create :rental    {:rental-ad rental-ad
                                      :renter    user})
        response  (get-rental-details renter rental)]
  (testing "car owner is business owner"
    (plait [owner :business]
      (is (= 2 (:vat-rate response)))))
  (testing "car owner is private"
    (plait [owner :private]
      (is (= 1 (:vat-rate response))))))
```

so complex relationships can be captured once and the difference
between tests can be expressed in a much shorter manner.

## Known limitations

The implementation uses `clojure.walk/macroexpand-all` which means
that whenever `plait` is used the compilation is taken over by
`plait`. In tests, this means that the meta-information about which
line a failing test is on is lost and e.g. cider can no longer
navigate directly to a failing assertion but only to the toplevel
`deftest`.

## Thanks

Thanks to Jacob Tjørnholm for coming up with the idea and Filip
Krasnianski for pushing the idea further.

## License

Released under Apache License, Version 2.0.
package automaton

import zio.*
import zio.json.{DeriveJsonDecoder, JsonDecoder}

import scala.quoted.*

final case class Handler[Service](
    description: String,
    call: (Service, RpcCall) => Task[Any]
)

final case class RpcCall(rpc: String, args: Array[Any])

object RpcCall:
  // TODO: Allow for more complex types
  private final case class RpcCallOption(rpc: String, args: Option[Array[Any]])
  // { "rpc": "methodName",  "args": [1, "hello", true] }
  given JsonDecoder[RpcCall] =
    given anyDecoder: JsonDecoder[Any] =
      JsonDecoder.string.widen[Any] orElse
        JsonDecoder.int.widen[Any] orElse
        JsonDecoder.long.widen[Any] orElse
        JsonDecoder.boolean.widen[Any] orElse
        JsonDecoder.double.widen[Any] orElse
        JsonDecoder.float.widen[Any]

    JsonDecoder.derived[RpcCallOption].map { case RpcCallOption(rpc, args) =>
      RpcCall(rpc, args.getOrElse(Array.empty))
    }

object ActionMacro:

  inline def run[A]: Handler[A] = ${ getTraitMethods[A] }

  def getTraitMethods[A: Type](using Quotes): Expr[Handler[A]] =
    import quotes.reflect.*
    val methods =
      TypeRepr.of[A].typeSymbol.declaredMethods.filterNot(p => p.flags.is(Flags.Synthetic) || p.flags.is(Flags.Private))

    def showTypeRepr(tpe: TypeRepr): String =
      tpe match
        case TypeRef(qual, name) =>
          name
        case other =>
          other.show

    def describeMethod(symbol: Symbol): String =

      val resultType0 = symbol.termRef.widenTermRefByName match
        case MethodType(_, _, res) =>
          res.widen.dealias
        case other => other

      val resultType = resultType0.asType match
        case '[ZIO[r, e, a]] => showTypeRepr(TypeRepr.of[a])
        case '[t]            => showTypeRepr(TypeRepr.of[t])

      // def methodName(arg1: String, arg2: Int): String
      s"""def ${symbol.name}(${symbol.paramSymss.flatten
          .map { param =>
            s"${param.name}: ${showTypeRepr(param.termRef.widenTermRefByName)}"
          }
          .mkString(", ")}): ${resultType}"""

    val description = methods.map(describeMethod).mkString("\n")

    val methodCalls =
      Expr.ofSeq(methods.map { method =>
        val methodCall = makeMethodCall[A](method)
        '{ ${ Expr(method.name) } -> ${ methodCall } }
      })

    val methodMap = '{ $methodCalls.toMap }

    val call = '{ (a: A, rpcCall: RpcCall) =>
      $methodMap(rpcCall.rpc).apply(a, rpcCall.args)
    }

//    report.errorAndAbort(s"Description: $description")

    '{ Handler[A](${ Expr(description) }, $call) }

  def makeMethodCall[A: Type](using Quotes)(method: quotes.reflect.Symbol): Expr[(A, Array[Any]) => Task[Any]] =
    import quotes.reflect.*

    val params = method.paramSymss.flatten

    def makeCall(a: Expr[A], args: Expr[Array[Any]]) =
      val mappedArgs: List[Term] = params.indices.map { idx =>
        params(idx).termRef.widenTermRefByName.asType match
          case '[tpe] => '{ $args(${ Expr(idx) }).asInstanceOf[tpe] }.asTerm
      }.toList

      if method.paramSymss.isEmpty then
        Select
          .unique(a.asTerm, method.name)
          .asExprOf[Task[Any]]
      else
        Select
          .unique(a.asTerm, method.name)
          .appliedToArgs(mappedArgs)
          .asExprOf[Task[Any]]

    val call = '{ (a: A, args: Array[Any]) =>
      ${ makeCall('a, 'args) }
    }
    call

//  def paramToPrimitive(using Quotes)(param: quotes.reflect.Symbol): ActionSchema.Primitive =
//    param.termRef.widenTermRefByName.asType match
//      case '[String] => ActionSchema.Primitive("String")
//      case '[Int]    => ActionSchema.Primitive("Int")
//      case '[Long]   => ActionSchema.Primitive("Long")
